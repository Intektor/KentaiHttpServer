package de.intektor.kentai_http_server.handlers

import de.intektor.kentai_http_common.client_to_server.CheckUsernameAvailableRequestToServer
import de.intektor.kentai_http_common.gson.genGson
import de.intektor.kentai_http_common.server_to_client.CheckUsernameAvailableResponseToClient
import de.intektor.kentai_http_server.DatabaseConnection
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author Intektor
 */
class CheckUsernameAvailableRequestToServerHandler : AbstractHandler() {

    override fun handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
        response.contentType = "application/json; charset=utf-8"
        response.status = HttpServletResponse.SC_OK

        val reqGson = genGson()
        val req = reqGson.fromJson(request.reader, CheckUsernameAvailableRequestToServer::class.java)

        DatabaseConnection.ds.connection.use { connection ->
            val statement = connection.prepareStatement("SELECT username FROM kentai.login_table WHERE username = ?")
            statement.setString(1, req.username)
            val query = statement.executeQuery()
            val res = CheckUsernameAvailableResponseToClient(!query.next())
            val resGson = genGson()
            resGson.toJson(res, response.writer)
        }
        baseRequest.isHandled = true
    }
}