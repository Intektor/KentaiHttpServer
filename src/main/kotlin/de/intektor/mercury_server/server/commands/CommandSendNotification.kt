package de.intektor.mercury_server.server.commands

import com.google.gson.stream.JsonWriter
import de.intektor.mercury_common.util.FCMMessageType
import de.intektor.mercury_server.DatabaseConnection
import de.intektor.mercury_server.MercuryServer
import de.intektor.mercury_server.server.Command
import okhttp3.MediaType
import okhttp3.RequestBody
import java.io.StringWriter

/**
 * @author Intektor
 */
class CommandSendNotification : Command {

    override fun execute(name: String, args: List<String>) {
        if (args.size < 2) {
            logHelp()
            return
        }
        DatabaseConnection.ds.connection.use { connection ->
            connection.prepareStatement("SELECT fcm_token FROM mercury.login_table WHERE username = ? OR user_uuid = ?").use {
                it.setString(1, args[0])
                it.setString(2, args[0])
                val query = it.executeQuery()
                if (!query.next()) {
                    MercuryServer.logger.info("No user found with uuid or username: ${args[0]}!")
                    return
                } else {
                    val messageList = args.subList(1, args.size)
                    val builder = StringBuilder()
                    messageList.forEach { builder.append(it) }
                    sendNotification(query.getString(1), builder.toString())
                }
                query.close()
            }
        }
    }

    override fun getNames(): List<String> = listOf("sendNotification")

    override fun logHelp() {
        MercuryServer.logger.info("userUUID/username message")
    }

    private fun sendNotification(fcmToken: String, message: String) {
        val stringWriter = StringWriter()
        val writer = JsonWriter(stringWriter)
        writer.beginObject()

        writer.name("priority").value("high")

        writer.name("notification").beginObject()
        writer.name("title").value("Mercury")
        writer.name("body").value(message)
//        writer.name("icon").value("new")
        writer.endObject()

        writer.name("data").beginObject()
        writer.name("type").value(FCMMessageType.SERVER_NOTIFICATION.ordinal.toString())
        writer.endObject()

        writer.name("to").value(fcmToken)

        writer.endObject()
        writer.flush()

        val written = stringWriter.toString()
        println(written)
        val body = RequestBody.create(MediaType.parse("application/json"), written)
        val request = okhttp3.Request.Builder()
                .addHeader("Authorization", "key=AAAAFRPXCfU:APA91bFkjRPKGL_fHEqz0LNCI0PunZyf_Cv1YMKkBu6iN6fNUy4zkLG-p5BU81B8kS9PSnKJ0Y5WM-fq5Kj0nblenkm9JiTOE57MlAqWa57Li8eBNSvmoLgT1eskEDcpT0jFhPnnwTI7")
                .url("https://fcm.googleapis.com/fcm/send")
                .post(body)
                .build()
        val response = MercuryServer.httpClient.newCall(request).execute()
        println(response)
        response.close()
    }
}