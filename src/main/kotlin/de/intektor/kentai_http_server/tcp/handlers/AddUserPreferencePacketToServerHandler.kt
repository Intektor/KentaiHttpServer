package de.intektor.kentai_http_server.tcp.handlers

import de.intektor.kentai_http_common.tcp.IPacketHandler
import de.intektor.kentai_http_common.tcp.client_to_server.AddUserPreferencePacketToServer
import de.intektor.kentai_http_common.tcp.sendPacket
import de.intektor.kentai_http_common.tcp.server_to_client.Status
import de.intektor.kentai_http_common.tcp.server_to_client.UserChange
import de.intektor.kentai_http_common.tcp.server_to_client.UserStatusChangePacketToClient
import de.intektor.kentai_http_common.util.toUUID
import de.intektor.kentai_http_server.DatabaseConnection
import de.intektor.kentai_http_server.DirectConnector
import de.intektor.kentai_http_server.LastOnlineType
import java.io.DataOutputStream
import java.net.Socket

/**
 * @author Intektor
 */
class AddUserPreferencePacketToServerHandler : IPacketHandler<AddUserPreferencePacketToServer> {

    override fun handlePacket(packet: AddUserPreferencePacketToServer, socketFrom: Socket) {
        if (!DirectConnector.interestedMap.containsKey(packet.userUUID)) {
            DirectConnector.interestedMap.put(packet.userUUID, mutableListOf())
        }
        DirectConnector.interestedMap[packet.userUUID]!!.add(socketFrom)

        DirectConnector.preferenceMap[socketFrom]!!.add(packet.userUUID)

        val responseList = mutableListOf<UserChange>()

        if (DirectConnector.clientMap.containsKey(packet.userUUID)) {
            responseList.add(UserChange(packet.userUUID, Status.ONLINE, System.currentTimeMillis()))
        } else {
            DatabaseConnection.ds.connection.use { connection ->
                connection.prepareStatement("SELECT last_time_online, type_closed, user_uuid FROM kentai.user_status_table WHERE user_uuid = '${packet.userUUID}'").executeQuery().use { query ->
                    while (query.next()) {
                        val lastTimeOnline = query.getLong(1)
                        val typeClosed = LastOnlineType.values()[query.getInt(2)]
                        val userUUID = query.getString(3).toUUID()

                        responseList.add(UserChange(userUUID, if (typeClosed == LastOnlineType.CONNECTION_CLOSED) Status.OFFLINE_CLOSED else Status.OFFLINE_EXCEPTION, lastTimeOnline))
                    }
                }
            }
        }
        sendPacket(UserStatusChangePacketToClient(responseList), DataOutputStream(socketFrom.getOutputStream()))
    }
}