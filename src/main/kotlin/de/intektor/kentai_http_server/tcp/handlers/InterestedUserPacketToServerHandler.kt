package de.intektor.kentai_http_server.tcp.handlers

import de.intektor.kentai_http_common.tcp.IPacketHandler
import de.intektor.kentai_http_common.tcp.client_to_server.InterestedUserPacketToServer
import de.intektor.kentai_http_common.tcp.sendPacket
import de.intektor.kentai_http_common.tcp.server_to_client.ProfilePictureUpdatedPacketToClient
import de.intektor.kentai_http_common.tcp.server_to_client.Status
import de.intektor.kentai_http_common.tcp.server_to_client.UserChange
import de.intektor.kentai_http_common.tcp.server_to_client.UserStatusChangePacketToClient
import de.intektor.kentai_http_common.util.toUUID
import de.intektor.kentai_http_server.DatabaseConnection
import de.intektor.kentai_http_server.DirectConnector
import de.intektor.kentai_http_server.LastOnlineType
import de.intektor.kentai_http_server.hasProfilePictureBeenUpdated
import java.io.DataOutputStream
import java.net.Socket

/**
 * @author Intektor
 */
class InterestedUserPacketToServerHandler : IPacketHandler<InterestedUserPacketToServer> {

    override fun handlePacket(packet: InterestedUserPacketToServer, socketFrom: Socket) {
        val userUUID = packet.interestedUser.userUUID
        if (!DirectConnector.interestedMap.containsKey(userUUID)) {
            DirectConnector.interestedMap[userUUID] = mutableListOf()
        }

        if (packet.interested) {
            DirectConnector.interestedMap[userUUID]!! += socketFrom
            DirectConnector.preferenceMap[socketFrom]!! += userUUID

            val responseList = mutableListOf<UserChange>()

            if (DirectConnector.clientMap.containsKey(userUUID)) {
                responseList += UserChange(userUUID, Status.ONLINE, System.currentTimeMillis())
            } else {
                DatabaseConnection.ds.connection.use { connection ->
                    connection.prepareStatement("SELECT last_time_online, type_closed, user_uuid FROM kentai.user_status_table WHERE user_uuid = '$userUUID'").executeQuery().use { query ->
                        while (query.next()) {
                            val lastTimeOnline = query.getLong(1)
                            val typeClosed = LastOnlineType.values()[query.getInt(2)]
                            val userUUID = query.getString(3).toUUID()

                            responseList += UserChange(userUUID, if (typeClosed == LastOnlineType.CONNECTION_CLOSED) Status.OFFLINE_CLOSED else Status.OFFLINE_EXCEPTION, lastTimeOnline)
                        }
                    }
                }
            }
            val dataOut = DataOutputStream(socketFrom.getOutputStream())
            sendPacket(UserStatusChangePacketToClient(responseList), dataOut)

            if (hasProfilePictureBeenUpdated(userUUID, packet.interestedUser.lastTimeProfilePictureUpdate)) {
                sendPacket(ProfilePictureUpdatedPacketToClient(userUUID), dataOut)
            }
        } else {
            DirectConnector.interestedMap[userUUID]!! -= socketFrom
            DirectConnector.preferenceMap[socketFrom]!! -= userUUID
        }
    }
}