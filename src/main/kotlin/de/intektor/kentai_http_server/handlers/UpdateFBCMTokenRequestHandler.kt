package de.intektor.kentai_http_server.handlers

import de.intektor.kentai_http_common.client_to_server.UpdateFBCMTokenRequest
import de.intektor.kentai_http_common.gson.genGson
import de.intektor.kentai_http_common.util.decryptRSA
import de.intektor.kentai_http_common.util.toKey
import de.intektor.kentai_http_server.DatabaseConnection
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import java.security.Key
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author Intektor
 */
class UpdateFBCMTokenRequestHandler : AbstractHandler() {

    override fun handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
        response.contentType = "application/json; charset=utf-8"
        response.status = HttpServletResponse.SC_OK

        val reqGson = genGson()
        val req = reqGson.fromJson(request.reader, UpdateFBCMTokenRequest::class.java)
        DatabaseConnection.ds.connection.use { connection ->
            var auth_key: Key? = null
            connection.prepareStatement("SELECT auth_key FROM kentai.login_table WHERE user_uuid = ?").use { statement ->
                statement.setString(1, req.userUUID.toString())
                val query = statement.executeQuery()
                if (query.next()) {
                    auth_key = query.getString("auth_key").toKey()
                } else {
                    return
                }
            }
            if (auth_key == null) return

            val token = req.encryptedToken.decryptRSA(auth_key!!)

            connection.prepareStatement("UPDATE kentai.login_table SET fcm_token = ? WHERE user_uuid = ?").use { statement ->
                statement.setString(1, token)
                statement.setString(2, req.userUUID.toString())
                statement.execute()
            }
        }
        baseRequest.isHandled = true
    }
}