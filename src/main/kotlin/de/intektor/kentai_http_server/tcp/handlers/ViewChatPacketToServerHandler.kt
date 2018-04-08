package de.intektor.kentai_http_server.tcp.handlers

import de.intektor.kentai_http_common.tcp.IPacketHandler
import de.intektor.kentai_http_common.tcp.client_to_server.ViewChatPacketToServer
import de.intektor.kentai_http_server.DirectConnector
import java.net.Socket

/**
 * @author Intektor
 */
class ViewChatPacketToServerHandler : IPacketHandler<ViewChatPacketToServer> {

    override fun handlePacket(packet: ViewChatPacketToServer, socketFrom: Socket) {
        DirectConnector.userViewChat(DirectConnector.socketMap[socketFrom]!!, packet.chatUUID, packet.view)
    }
}