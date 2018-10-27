package de.intektor.mercury_server.handlers

import com.google.gson.stream.JsonWriter
import de.intektor.mercury_common.client_to_server.SendChatMessageRequest
import de.intektor.mercury_common.gson.genGson
import de.intektor.mercury_common.server_to_client.SendChatMessageResponse
import de.intektor.mercury_common.util.FCMMessageType
import de.intektor.mercury_common.util.toKey
import de.intektor.mercury_common.util.verify
import de.intektor.mercury_server.DatabaseConnection
import de.intektor.mercury_server.MercuryServer
import de.intektor.mercury_server.util.Logger
import de.intektor.mercury_server.util.setUUID
import okhttp3.MediaType
import okhttp3.RequestBody
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import java.io.StringWriter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author Intektor
 */
class SendChatMessageRequestHandler : AbstractHandler() {

    private companion object {
        private const val TAG = "SendChatMessageRequestHandler"
    }

    override fun handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
        response.contentType = "application/json; charset=utf-8"
        response.status = HttpServletResponse.SC_OK

        val reqGson = genGson()
        val req = reqGson.fromJson(request.reader, SendChatMessageRequest::class.java)

        DatabaseConnection.ds.connection.use { connection ->
            val senderAuthKey = connection.prepareStatement("SELECT auth_key, username FROM mercury.login_table WHERE user_uuid = ?").use { statement ->
                statement.setString(1, req.senderUUID.toString())
                statement.executeQuery().use { query ->
                    if (!query.next()) {
                        Logger.info(TAG, "Received message from user that is not registered in database, userUUID=${req.senderUUID}")
                        return
                    }
                    query.getString("auth_key").toKey()
                }
            }

            if (!verify(req.senderUUID.toString(), req.signature, senderAuthKey)) {
                Logger.info(TAG, "Received message from user but signature was wrong, senderUUID=${req.senderUUID}, ip=${request.remoteAddr}")
                return
            }

            for (message in req.list) {
                //Says if this message was sent before and so we don't want to send it again
                val valid = connection.prepareStatement("SELECT COUNT(message_uuid) FROM mercury.pending_messages WHERE message_uuid = ?").use { statement ->
                    statement.setUUID(1, message.messageUUID)
                    statement.executeQuery().use { query ->
                        query.next()
                        query.getInt(1) != 1
                    }
                }

                if (valid) {
                    connection.prepareStatement("INSERT INTO mercury.pending_messages (message_uuid, aes_key, init_vector, signature, sender_uuid, chat_uuid, receiver_uuid, data) VALUES(?, ?, ?, ?, ?, ?, ?, ?)").use { statement ->
                        statement.setUUID(1, message.messageUUID)
                        statement.setString(2, message.aesKey)
                        statement.setString(3, message.initVector)
                        statement.setBytes(4, message.signature)
                        statement.setUUID(5, req.senderUUID)
                        statement.setString(6, message.chatUUID)
                        statement.setUUID(7, message.receiverUUID)
                        statement.setString(8, message.message)
                        statement.execute()
                    }

                    val stringWriter = StringWriter()

                    connection.prepareStatement("SELECT fcm_token FROM mercury.login_table WHERE user_uuid = ?").use { statement ->
                        statement.setUUID(1, message.receiverUUID)
                        statement.executeQuery().use { query ->
                            if (query.next()) {
                                val fcmToken = query.getString("fcm_token")

                                val writer = JsonWriter(stringWriter)
                                writer.beginObject()

                                writer.name("priority").value("high")

                                writer.name("data").beginObject()
                                writer.name("type").value(FCMMessageType.CHAT_MESSAGE.ordinal.toString())
                                writer.endObject()

                                writer.name("to").value(fcmToken)

                                writer.endObject()
                                writer.flush()
                            }
                        }
                    }
                    val written = stringWriter.toString()
                    val body = RequestBody.create(MediaType.parse("application/json"), written)
                    val httpRequest = okhttp3.Request.Builder()
                            .addHeader("Authorization", "key=${MercuryServer.fcmKey}")
                            .url("https://fcm.googleapis.com/fcm/send")
                            .post(body)
                            .build()
                    val httpResponse = MercuryServer.httpClient.newCall(httpRequest).execute()
                    httpResponse.close()
                }
            }
        }
        val res = SendChatMessageResponse(0)
        val resGson = genGson()
        resGson.toJson(res, response.writer)
        baseRequest.isHandled = true
    }
}