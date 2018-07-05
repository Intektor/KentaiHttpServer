package de.intektor.kentai_http_server.handlers

import de.intektor.kentai_http_common.client_to_server.UsersRequest
import de.intektor.kentai_http_common.gson.genGson
import de.intektor.kentai_http_common.server_to_client.UsersResponse
import de.intektor.kentai_http_common.util.toKey
import de.intektor.kentai_http_common.util.toUUID
import de.intektor.kentai_http_server.DatabaseConnection
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import java.security.interfaces.RSAPublicKey
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author Intektor
 */
class UsersRequestHandler : AbstractHandler() {

    override fun handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
        val gson = genGson()
        val r = gson.fromJson(request.reader, UsersRequest::class.java)

        DatabaseConnection.ds.connection.use { connection ->
            connection.prepareStatement("SELECT user_uuid, username, message_key FROM kentai.login_table WHERE user_uuid IN(${"?, ".repeat(r.users.size).dropLast(2)})").use { statement1 ->
                for ((i, uuid) in r.users.withIndex()) {
                    statement1.setString(i + 1, uuid.toString())
                }
                val list = statement1.executeQuery().use { query ->
                    val list = ArrayList<UsersResponse.UserResponse>(r.users.size)
                    var i = 0
                    while (query.next()) {
                        val userUUID = query.getString("user_uuid").toUUID()
                        val username = query.getString("username")
                        val messageKey = query.getString("message_key").toKey() as RSAPublicKey
                        list += UsersResponse.UserResponse(userUUID, username, messageKey)
                        i++
                    }
                    list
                }
                gson.toJson(UsersResponse(list), response.writer)
                baseRequest.isHandled = true
            }
        }
    }
}