package de.intektor.mercury_server.handlers

import de.intektor.mercury_common.client_to_server.HandledMessagesRequest
import de.intektor.mercury_common.gson.genGson
import de.intektor.mercury_common.util.toKey
import de.intektor.mercury_common.util.verify
import de.intektor.mercury_server.DatabaseConnection
import de.intektor.mercury_server.util.Logger
import de.intektor.mercury_server.util.setUUID
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author Intektor
 */
class HandledMessagesRequestHandler : AbstractHandler() {

    private companion object {
        private const val TAG = "HandledMessagesRequestHandler"
    }

    override fun handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
        val r = genGson().fromJson(request.reader, HandledMessagesRequest::class.java)
        DatabaseConnection.ds.connection.use { connection ->
            val authKey = connection.prepareStatement("SELECT auth_key FROM mercury.login_table WHERE user_uuid = ?").use { statement ->
                statement.setUUID(1, r.userUUID)
                statement.executeQuery().use { query ->
                    query.next()
                    query.getString("auth_key").toKey()
                }

            }

            if (!verify(r.userUUID.toString(), r.signature, authKey)) {
                Logger.info(TAG, "A user tried to set messages handled but his signature could not be verified. userUUID=${r.userUUID}")
                return
            }

            val messageList = r.messageList.distinctBy { it }
            connection.prepareStatement("DELETE FROM mercury.pending_messages WHERE receiver_uuid = ? AND message_uuid IN (${"?, ".repeat(messageList.size).dropLast(2)})").use { statement ->
                statement.setUUID(1, r.userUUID)
                for ((index, uuid) in messageList.withIndex()) {
                    statement.setUUID(index + 2, uuid)
                }
                statement.execute()
            }
        }
        baseRequest.isHandled = true
    }
}