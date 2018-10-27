package de.intektor.mercury_server.tcp.handlers

import de.intektor.mercury_common.tcp.IPacketHandler
import de.intektor.mercury_common.tcp.client_to_server.ViewChatPacketToServer
import de.intektor.mercury_server.DirectConnector
import java.net.Socket

/**
 * @author Intektor
 */
class ViewChatPacketToServerHandler : IPacketHandler<ViewChatPacketToServer> {

    override fun handlePacket(packet: ViewChatPacketToServer, socketFrom: Socket) {
        DirectConnector.userViewChat(DirectConnector.socketMap[socketFrom]!!, packet.chatUUID, packet.view)
    }
}