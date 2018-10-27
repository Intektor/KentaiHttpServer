package de.intektor.mercury_server.tcp.handlers

import de.intektor.mercury_common.tcp.IPacketHandler
import de.intektor.mercury_common.tcp.client_to_server.TypingPacketToServer
import de.intektor.mercury_common.tcp.sendPacket
import de.intektor.mercury_common.tcp.server_to_client.TypingPacketToClient
import de.intektor.mercury_server.DirectConnector
import java.io.DataOutputStream
import java.net.Socket

/**
 * @author Intektor
 */
class TypingPacketToServerHandler : IPacketHandler<TypingPacketToServer> {

    override fun handlePacket(packet: TypingPacketToServer, socketFrom: Socket) {
        val typingPacket = TypingPacketToClient(DirectConnector.socketMap[socketFrom]!!, packet.chatUUID)
        packet.sendTo
                .mapNotNull { DirectConnector.clientMap[it] }
                .forEach { sendPacket(typingPacket, DataOutputStream(it.getOutputStream())) }
    }

}