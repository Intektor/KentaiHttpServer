package de.intektor.kentai_http_server.handlers

import de.intektor.kentai_http_common.client_to_server.FetchMessageRequest
import de.intektor.kentai_http_common.gson.genGson
import de.intektor.kentai_http_common.server_to_client.FetchMessageResponse
import de.intektor.kentai_http_common.util.decryptRSA
import de.intektor.kentai_http_common.util.toKey
import de.intektor.kentai_http_server.DatabaseConnection
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author Intektor
 */
class FetchMessageRequestHandler : AbstractHandler() {

    override fun handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
        response.contentType = "application/json; charset=utf-8"
        response.status = HttpServletResponse.SC_OK

        val reqGson = genGson()
        val req = reqGson.fromJson(request.reader, FetchMessageRequest::class.java)

        DatabaseConnection.ds.connection.use { connection ->
            connection.prepareStatement("SELECT auth_key FROM kentai.login_table WHERE user_uuid = ?").use { statement1 ->
                statement1.setString(1, req.userUUID.toString())
                val query = statement1.executeQuery()
                query.next()
                val authKey = query.getString("auth_key").toKey()
                query.close()

                val messageUUID = UUID.fromString(req.encryptedMessageUUID.decryptRSA(authKey))
                connection.prepareStatement("SELECT text, reference, registry_id, time_sent, aes_key, init_vector FROM kentai.pending_messages WHERE message_uuid = ?").use { statement2 ->
                    statement2.setString(1, messageUUID.toString())
                    val query2 = statement2.executeQuery()
                    query2.next()
                    val text = query2.getString("text")
                    val reference: String? = query2.getString("reference")
                    val timeSent = query2.getLong("time_sent")
                    val aesKey = query2.getString("aes_key")
                    val initVector = query2.getString("init_vector")
                    query2.close()

                    val res = FetchMessageResponse(text, reference, timeSent, aesKey, initVector)
                    val resGson = genGson()
                    resGson.toJson(res, response.writer)
                }
            }
        }
        baseRequest.isHandled = true
    }
}