package de.intektor.kentai_http_server.handlers

import com.google.common.io.BaseEncoding
import de.intektor.kentai_http_common.client_to_server.FetchMessageRequest
import de.intektor.kentai_http_common.gson.genGson
import de.intektor.kentai_http_common.server_to_client.FetchMessageResponse
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
class FetchMessageRequestHandler : AbstractHandler() {

    override fun handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
        response.contentType = "application/json; charset=utf-8"
        response.status = HttpServletResponse.SC_OK

        val reqGson = genGson()
        val req = reqGson.fromJson(request.reader, FetchMessageRequest::class.java)
        DatabaseConnection.ds.connection.use { connection ->
            connection.prepareStatement("SELECT auth_key FROM kentai.login_table WHERE user_uuid = ?").use { statement1 ->
                statement1.setString(1, req.userUUID)
                val query = statement1.executeQuery()
                query.next()
                val authKey = query.getString("auth_key").toKey()
                query.close()

                val decryptedUserUUID = req.encryptedUserUUID.decryptRSA(authKey)

                if (req.userUUID == decryptedUserUUID) {
                    val lastTime = connection.prepareStatement("SELECT text, reference, registry_id, time_sent, aes_key, init_vector, small_data, signature, message_uuid, registry_id, sender_uuid, chat_uuid, kentai.login_table.username AS username FROM kentai.pending_messages LEFT JOIN kentai.login_table ON kentai.login_table.user_uuid = kentai.pending_messages.sender_uuid WHERE receiver_uuid = ? ORDER BY time_sent ASC").use { statement2 ->
                        statement2.setString(1, decryptedUserUUID)
                        statement2.executeQuery().use { query2 ->
                            val list = mutableListOf<FetchMessageResponse.Message>()
                            while (query2.next()) {
                                val text = query2.getString("text")
                                val reference: String = query2.getString("reference")
                                val timeSent = query2.getLong("time_sent")
                                val aesKey = query2.getString("aes_key")
                                val initVector = query2.getString("init_vector")
                                val smallData = query2.getBytes("small_data")
                                val signature = query2.getString("signature")
                                val chatUUID = query2.getString("chat_uuid")
                                val messageUUID = query2.getString("message_uuid")
                                val senderUUID = query2.getString("sender_uuid")
                                val registryID = query2.getString("registry_id")
                                val senderUsername = query2.getString("username")
                                val message = FetchMessageResponse.Message(text, reference, timeSent, aesKey,
                                        initVector, if (smallData != null) BaseEncoding.base64().encode(smallData) else null, signature,
                                        chatUUID, senderUUID, registryID, messageUUID, senderUsername)
                                list.add(message)
                            }

                            val res = FetchMessageResponse(list)
                            val resGson = genGson()
                            resGson.toJson(res, response.writer)

                            if (list.isNotEmpty()) {
                                list.last().timeSent
                            } else {
                                -1
                            }
                        }
                    }
                }
            }
        }
        baseRequest.isHandled = true
    }
}