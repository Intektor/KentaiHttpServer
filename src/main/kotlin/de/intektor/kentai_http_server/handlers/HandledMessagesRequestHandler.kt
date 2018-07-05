package de.intektor.kentai_http_server.handlers

import de.intektor.kentai_http_common.client_to_server.HandledMessagesRequest
import de.intektor.kentai_http_common.gson.genGson
import de.intektor.kentai_http_common.util.decryptRSA
import de.intektor.kentai_http_common.util.toKey
import de.intektor.kentai_http_server.DatabaseConnection
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author Intektor
 */
class HandledMessagesRequestHandler : AbstractHandler() {

    override fun handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
        val r = genGson().fromJson(request.reader, HandledMessagesRequest::class.java)
        DatabaseConnection.ds.connection.use { connection ->
            val authKey = connection.prepareStatement("SELECT auth_key FROM kentai.login_table WHERE user_uuid = ?").use { statement1 ->
                statement1.setString(1, r.userUUID)
                statement1.executeQuery().use { query ->
                    query.next()
                    query.getString("auth_key").toKey()
                }

            }
            val decryptedUserUUID = r.encryptedUserUUID.decryptRSA(authKey)

            val messageList = r.messageList.distinctBy { it }
            connection.prepareStatement("DELETE FROM kentai.pending_messages WHERE receiver_uuid = ? AND message_uuid IN (${"?, ".repeat(messageList.size).dropLast(2)})").use { statement ->
                statement.setString(1, decryptedUserUUID)
                for ((index, uuid) in messageList.withIndex()) {
                    statement.setString(index + 2, uuid)
                }
                statement.execute()
            }
        }
        baseRequest.isHandled = true
    }
}