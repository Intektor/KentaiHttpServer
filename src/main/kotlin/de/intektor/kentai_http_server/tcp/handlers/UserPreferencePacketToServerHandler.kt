package de.intektor.kentai_http_server.tcp.handlers

import de.intektor.kentai_http_common.tcp.IPacketHandler
import de.intektor.kentai_http_common.tcp.client_to_server.UserPreferencePacketToServer
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
import java.util.*

/**
 * @author Intektor
 */
class UserPreferencePacketToServerHandler : IPacketHandler<UserPreferencePacketToServer> {

    override fun handlePacket(packet: UserPreferencePacketToServer, socketFrom: Socket) {
        for (user in packet.list) {
            val uuid = user.userUUID
            if (!DirectConnector.interestedMap.containsKey(uuid)) {
                DirectConnector.interestedMap[uuid] = mutableListOf()
            }
            DirectConnector.interestedMap[uuid]!! += socketFrom
        }
        DirectConnector.preferenceMap[socketFrom] = packet.list.map { it.userUUID }.toMutableList()

        val responseList = mutableListOf<UserChange>()
        val todoList = mutableListOf<UUID>()
        for (user in packet.list) {
            val uuid = user.userUUID
            if (DirectConnector.clientMap.containsKey(uuid)) {
                responseList.add(UserChange(uuid, Status.ONLINE, System.currentTimeMillis()))
            } else {
                todoList.add(uuid)
            }
        }

        if (todoList.isNotEmpty()) {
            DatabaseConnection.ds.connection.use { connection ->
                val stringBuilder = StringBuilder()

                for ((i, uuid) in todoList.withIndex()) {
                    stringBuilder.append('\'')
                    stringBuilder.append(uuid.toString())
                    stringBuilder.append('\'')

                    if (i != todoList.size - 1) {
                        stringBuilder.append(", ")
                    }
                }

                val inClause = stringBuilder.toString()

                connection.prepareStatement("SELECT last_time_online, type_closed, user_uuid FROM kentai.user_status_table WHERE user_uuid IN ($inClause)").executeQuery().use { query ->
                    while (query.next()) {
                        val lastTimeOnline = query.getLong(1)
                        val typeClosed = LastOnlineType.values()[query.getInt(2)]
                        val userUUID = query.getString(3).toUUID()

                        responseList.add(UserChange(userUUID, if (typeClosed == LastOnlineType.CONNECTION_CLOSED) Status.OFFLINE_CLOSED else Status.OFFLINE_EXCEPTION, lastTimeOnline))
                    }
                }
            }
        }

        val dataOut = DataOutputStream(socketFrom.getOutputStream())
        sendPacket(UserStatusChangePacketToClient(responseList), dataOut)

        for (interestedUser in packet.list) {
            if (hasProfilePictureBeenUpdated(interestedUser.userUUID, interestedUser.lastTimeProfilePictureUpdate)) {
                sendPacket(ProfilePictureUpdatedPacketToClient(interestedUser.userUUID), dataOut)
            }
        }
    }
}