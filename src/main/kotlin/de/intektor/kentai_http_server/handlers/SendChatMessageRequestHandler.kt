package de.intektor.kentai_http_server.handlers

import com.google.gson.stream.JsonWriter
import de.intektor.kentai_http_common.client_to_server.SendChatMessageRequest
import de.intektor.kentai_http_common.gson.genGson
import de.intektor.kentai_http_common.server_to_client.SendChatMessageResponse
import de.intektor.kentai_http_common.util.FCMMessageType
import de.intektor.kentai_http_common.util.decryptRSA
import de.intektor.kentai_http_common.util.toKey
import de.intektor.kentai_http_server.DatabaseConnection
import de.intektor.kentai_http_server.KentaiServer
import okhttp3.MediaType
import okhttp3.RequestBody
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import java.io.StringWriter
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author Intektor
 */
class SendChatMessageRequestHandler : AbstractHandler() {

    override fun handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
        response.contentType = "application/json; charset=utf-8"
        response.status = HttpServletResponse.SC_OK

        val reqGson = genGson()
        val req = reqGson.fromJson(request.reader, SendChatMessageRequest::class.java)

        DatabaseConnection.ds.connection.use { connection ->
            for (message in req.list) {
                var receiverUUID: UUID
                connection.prepareStatement("SELECT auth_key, username FROM kentai.login_table WHERE user_uuid = ?").use { statement ->
                    statement.setString(1, message.senderUUID.toString())
                    val query = statement.executeQuery()
                    query.next()
                    receiverUUID = UUID.fromString(message.receiverUUIDEncrypted.decryptRSA(query.getString("auth_key").toKey()))
                    val senderUsername = query.getString("username")

                    //Says if this message was sent before and so we don't want to send it again
                    var valid = true

                    connection.prepareStatement("SELECT message_uuid FROM kentai.pending_messages WHERE message_uuid = ?").use { statement ->
                        statement.setString(1, message.message.id.toString())
                        val query = statement.executeQuery()
                        valid = !query.next()
                    }

                    if (valid) {
                        connection.prepareStatement("INSERT INTO kentai.pending_messages (message_uuid, text, reference, registry_id, time_sent, aes_key, init_vector) VALUES(?, ?, ?, ?, ?, ?, ?)").use { statement ->
                            statement.setString(1, message.message.id.toString())
                            statement.setString(2, message.message.text)
                            statement.setString(3, "")
                            statement.setString(4, message.messageRegistryId)
                            statement.setLong(5, message.message.timeSent)
                            statement.setString(6, message.message.aesKey)
                            statement.setString(7, message.message.initVector)
                            statement.execute()
                        }

                        val stringWriter = StringWriter()

                        connection.prepareStatement("SELECT fcm_token FROM kentai.login_table WHERE user_uuid = ?").use { statement ->
                            statement.setString(1, receiverUUID.toString())
                            val query = statement.executeQuery()
                            if (query.next()) {
                                val fcmToken = query.getString("fcm_token")

                                val writer = JsonWriter(stringWriter)
                                writer.beginObject()

                                writer.name("priority").value("high")

                                writer.name("data").beginObject()
                                writer.name("type").value(FCMMessageType.CHAT_MESSAGE.ordinal.toString())
                                writer.name("chat_uuid").value(message.chatUUID.toString())
                                writer.name("message_uuid").value(message.message.id.toString())
                                writer.name("sender_uuid").value(message.senderUUID.toString())
                                writer.name("sender_username").value(senderUsername)
                                writer.name("message_registry_id").value(message.messageRegistryId)
                                writer.name("preview_text").value(message.previewText)
                                writer.endObject()

                                writer.name("to").value(fcmToken)

                                writer.endObject()
                                writer.flush()
                            }
                        }
                        val written = stringWriter.toString()
                        println(written)
                        val body = RequestBody.create(MediaType.parse("application/json"), written)
                        val request = okhttp3.Request.Builder()
                                .addHeader("Authorization", "key=AAAAFRPXCfU:APA91bFkjRPKGL_fHEqz0LNCI0PunZyf_Cv1YMKkBu6iN6fNUy4zkLG-p5BU81B8kS9PSnKJ0Y5WM-fq5Kj0nblenkm9JiTOE57MlAqWa57Li8eBNSvmoLgT1eskEDcpT0jFhPnnwTI7")
                                .url("https://fcm.googleapis.com/fcm/send")
                                .post(body)
                                .build()
                        val response = KentaiServer.httpClient.newCall(request).execute()
                        println(response)
                        response.close()
                    }
                }
            }
        }
        val res = SendChatMessageResponse(0)
        val resGson = genGson()
        resGson.toJson(res, response.writer)
        baseRequest.isHandled = true
    }
}