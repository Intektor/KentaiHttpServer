package de.intektor.mercury_server.handlers

import de.intektor.mercury_common.client_to_server.UpdateFBCMTokenRequest
import de.intektor.mercury_common.gson.genGson
import de.intektor.mercury_common.server_to_client.UpdateFBCMTokenResponse
import de.intektor.mercury_common.util.toKey
import de.intektor.mercury_common.util.verify
import de.intektor.mercury_server.DatabaseConnection
import de.intektor.mercury_server.util.Logger
import de.intektor.mercury_server.util.setUUID
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author Intektor
 */
class UpdateFBCMTokenRequestHandler : AbstractHandler() {

    private companion object {
        private const val TAG = "UpdateFBCMTokenRequestHandler"
    }

    override fun handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
        response.contentType = "application/json; charset=utf-8"
        response.status = HttpServletResponse.SC_OK

        val gson = genGson()
        val req = gson.fromJson(request.reader, UpdateFBCMTokenRequest::class.java)
        DatabaseConnection.ds.connection.use { connection ->
            val authKey = connection.prepareStatement("SELECT auth_key FROM mercury.login_table WHERE user_uuid = ?").use { statement ->
                statement.setUUID(1, req.userUUID)
                val query = statement.executeQuery()
                if (!query.next()) {
                    return
                }
                query.getString("auth_key").toKey()

            }

            if (!verify(req.token.toByteArray(), req.signature, authKey)) {
                Logger.info(TAG, "A user tried to change his fcm token but the signature was wrong, userUUID=${req.userUUID}, token=${req.token}")
            }

            connection.prepareStatement("UPDATE mercury.login_table SET fcm_token = ? WHERE user_uuid = ?").use { statement ->
                statement.setString(1, req.token)
                statement.setUUID(2, req.userUUID)
                statement.execute()
            }
        }

        gson.toJson(UpdateFBCMTokenResponse(true), response.writer)

        baseRequest.isHandled = true
    }
}