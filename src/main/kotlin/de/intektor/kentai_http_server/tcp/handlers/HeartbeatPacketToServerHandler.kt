package de.intektor.kentai_http_server.tcp.handlers

import de.intektor.kentai_http_common.tcp.IPacketHandler
import de.intektor.kentai_http_common.tcp.client_to_server.HeartbeatPacketToServer
import de.intektor.kentai_http_common.tcp.sendPacket
import de.intektor.kentai_http_common.tcp.server_to_client.HeartbeatPacketToClient
import de.intektor.kentai_http_server.DirectConnector
import java.io.DataOutputStream
import java.net.Socket

/**
 * @author Intektor
 */
class HeartbeatPacketToServerHandler : IPacketHandler<HeartbeatPacketToServer> {

    override fun handlePacket(packet: HeartbeatPacketToServer, socketFrom: Socket) {
        DirectConnector.threadMap[socketFrom]?.lastTimeHeartbeat = System.currentTimeMillis()
        sendPacket(HeartbeatPacketToClient(), DataOutputStream(socketFrom.getOutputStream()))
    }
}