package de.intektor.kentai_http_server.handlers

import com.google.common.io.BaseEncoding
import de.intektor.kentai_http_common.client_to_server.RegisterRequestToServer
import de.intektor.kentai_http_common.gson.genGson
import de.intektor.kentai_http_common.server_to_client.RegisterRequestResponseToClient
import de.intektor.kentai_http_server.DatabaseConnection
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author Intektor
 */
class RegisterRequestToServerHandler : AbstractHandler() {
    override fun handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
        response.contentType = "application/json; charset=utf-8"
        response.status = HttpServletResponse.SC_OK

        val reqGson = genGson()
        val req = reqGson.fromJson(request.reader, RegisterRequestToServer::class.java)

        val resGson = genGson()

        if (req.username.length !in 5..20 || !req.username.matches(("\\w+".toRegex()))) {
            reqGson.toJson(RegisterRequestResponseToClient(req.username, null, RegisterRequestResponseToClient.Type.INVALID_USERNAME), response.writer)
        } else {
            DatabaseConnection.ds.connection.use { connection ->
                connection.prepareStatement("SELECT username FROM kentai.login_table WHERE username = ?").use { statement ->
                    statement.setString(1, req.username)
                    if (!statement.executeQuery().next()) {
                        val userUUID = UUID.randomUUID()

                        connection.prepareStatement("INSERT INTO kentai.login_table (username, user_uuid, message_key, auth_key, fcm_token) VALUES (?, ?, ?, ?, ?)").use { statement2 ->
                            statement2.setString(1, req.username)
                            statement2.setString(2, userUUID.toString())
                            statement2.setString(3, BaseEncoding.base64().encode(req.messageKey.encoded))
                            statement2.setString(4, BaseEncoding.base64().encode(req.authKey.encoded))
                            statement2.setString(5, req.fCMToken)
                            statement2.execute()
                        }

                        resGson.toJson(reqGson.toJson(RegisterRequestResponseToClient(req.username, userUUID, RegisterRequestResponseToClient.Type.SUCCESS), response.writer))
                    } else {
                        reqGson.toJson(RegisterRequestResponseToClient(req.username, null, RegisterRequestResponseToClient.Type.TAKEN_USERNAME), response.writer)
                    }
                }
            }
        }
    }
}