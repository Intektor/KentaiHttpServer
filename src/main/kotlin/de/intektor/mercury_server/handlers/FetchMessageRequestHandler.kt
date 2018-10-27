package de.intektor.mercury_server.handlers

import de.intektor.mercury_common.client_to_server.FetchMessageRequest
import de.intektor.mercury_common.gson.genGson
import de.intektor.mercury_common.server_to_client.FetchMessageResponse
import de.intektor.mercury_common.util.toKey
import de.intektor.mercury_common.util.verify
import de.intektor.mercury_server.DatabaseConnection
import de.intektor.mercury_server.util.Logger
import de.intektor.mercury_server.util.getUUID
import de.intektor.mercury_server.util.setUUID
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author Intektor
 */
class FetchMessageRequestHandler : AbstractHandler() {

    private companion object {
        private const val TAG = "FetchMessageRequestHandler"
    }

    override fun handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
        response.contentType = "application/json; charset=utf-8"
        response.status = HttpServletResponse.SC_OK

        val reqGson = genGson()
        val req = reqGson.fromJson(request.reader, FetchMessageRequest::class.java)
        DatabaseConnection.ds.connection.use { connection ->
            val authKey = connection.prepareStatement("SELECT auth_key FROM mercury.login_table WHERE user_uuid = ?").use { statement ->
                statement.setUUID(1, req.userUUID)
                statement.executeQuery().use { query ->
                    if (!query.next()) {
                        Logger.info(TAG, "A user wanted to fetch messages but he is not registered in database, userUUID=${req.userUUID}")
                        return
                    }
                    query.getString(1).toKey()
                }
            }

            if (!verify(req.userUUID.toString(), req.signature, authKey)) {
                Logger.info(TAG, "A user wanted to fetch messages from userUUID=${req.userUUID} but his signature couldn't be verified.")
            }

            connection.prepareStatement("SELECT aes_key, init_vector, signature, message_uuid, sender_uuid, chat_uuid, data FROM mercury.pending_messages " +
                    "WHERE receiver_uuid = ? ORDER BY time_sent ASC").use { statement ->
                statement.setUUID(1, req.userUUID)
                statement.executeQuery().use { query ->
                    val list = mutableListOf<FetchMessageResponse.Message>()
                    while (query.next()) {
                        val aesKey = query.getString(1)
                        val initVector = query.getString(2)
                        val signature = query.getBytes(3)
                        val messageUUID = query.getUUID(4)
                        val senderUUID = query.getUUID(5)
                        val chatUUID = query.getString(6)
                        val data = query.getString(7)

                        list += FetchMessageResponse.Message(data, messageUUID, aesKey, initVector, chatUUID, senderUUID, signature)
                    }

                    val res = FetchMessageResponse(list)
                    val resGson = genGson()
                    resGson.toJson(res, response.writer)
                }
            }
        }
        baseRequest.isHandled = true
    }

}