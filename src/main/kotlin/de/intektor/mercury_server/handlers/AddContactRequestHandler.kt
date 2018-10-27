package de.intektor.mercury_server.handlers

import de.intektor.mercury_common.client_to_server.AddContactRequest
import de.intektor.mercury_common.gson.genGson
import de.intektor.mercury_common.server_to_client.AddContactResponse
import de.intektor.mercury_common.util.toKey
import de.intektor.mercury_server.DatabaseConnection
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import java.security.interfaces.RSAPublicKey
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author Intektor
 */
class AddContactRequestHandler : AbstractHandler() {
    override fun handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
        response.contentType = "application/json; charset=utf-8"
        response.status = HttpServletResponse.SC_OK

        val reqGson = genGson()
        val req = reqGson.fromJson(request.reader, AddContactRequest::class.java)

        DatabaseConnection.ds.connection.use { connection ->
            val statement = connection.prepareStatement("SELECT message_key, user_uuid FROM mercury.login_table WHERE username = ?")
            statement.setString(1, req.username)
            val query = statement.executeQuery()

            val res = if (query.next()) {
                AddContactResponse(true, query.getString("message_key").toKey() as RSAPublicKey, UUID.fromString(query.getString("user_uuid")))
            } else {
                AddContactResponse(false, null, UUID.randomUUID())
            }

            val resGson = genGson()
            resGson.toJson(res, response.writer)
        }
        baseRequest.isHandled = true
    }
}
