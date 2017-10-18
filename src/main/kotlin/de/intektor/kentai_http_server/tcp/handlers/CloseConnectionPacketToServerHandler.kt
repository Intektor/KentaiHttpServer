package de.intektor.kentai_http_server.tcp.handlers

import de.intektor.kentai_http_common.tcp.IPacketHandler
import de.intektor.kentai_http_common.tcp.client_to_server.CloseConnectionPacketToServer
import de.intektor.kentai_http_server.DirectConnector
import java.net.Socket

/**
 * @author Intektor
 */
class CloseConnectionPacketToServerHandler : IPacketHandler<CloseConnectionPacketToServer> {

    override fun handlePacket(packet: CloseConnectionPacketToServer, socketFrom: Socket) {
        val uuid = DirectConnector.socketMap[socketFrom]!!

        DirectConnector.unregisterClient(uuid, true)
    }
}