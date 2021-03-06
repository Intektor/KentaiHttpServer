package de.intektor.mercury_server.tcp.handlers

import de.intektor.mercury_common.tcp.IPacketHandler
import de.intektor.mercury_common.tcp.client_to_server.IdentificationPacketToServer
import de.intektor.mercury_common.util.decryptRSA
import de.intektor.mercury_common.util.toKey
import de.intektor.mercury_server.DatabaseConnection
import de.intektor.mercury_server.DirectConnector
import java.net.Socket
import java.util.*

/**
 * @author Intektor
 */
class IdentificationPacketToServerHandler : IPacketHandler<IdentificationPacketToServer> {

    override fun handlePacket(packet: IdentificationPacketToServer, socketFrom: Socket) {
        try {
            DatabaseConnection.ds.connection.use { connection ->
                connection.prepareStatement("SELECT auth_key FROM mercury.login_table WHERE user_uuid = ?").use { statement ->
                    statement.setString(1, packet.userUUID.toString())
                    statement.executeQuery().use { query ->
                        if (query.next()) {
                            val authKey = query.getString("auth_key").toKey()
                            val decryptedUserUUID = packet.encryptedUserUUID.decryptRSA(authKey)
                            if (UUID.fromString(decryptedUserUUID) == packet.userUUID) {
                                DirectConnector.registerClient(packet.userUUID, socketFrom)
                            }
                        } else {
                            socketFrom.close()
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            socketFrom.close()
        }
    }
}