package de.intektor.kentai_http_server.handlers

import de.intektor.kentai_http_common.client_to_server.KeyRequest
import de.intektor.kentai_http_common.gson.genGson
import de.intektor.kentai_http_common.server_to_client.KeyResponse
import de.intektor.kentai_http_common.util.toKey
import de.intektor.kentai_http_server.DatabaseConnection
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import java.security.interfaces.RSAPublicKey
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author Intektor
 */
class KeyRequestHandler : AbstractHandler() {

    override fun handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
        DatabaseConnection.ds.connection.use { connection ->
            val gson = genGson()
            val req = gson.fromJson(request.reader, KeyRequest::class.java)

            if (req.requestedUUIDs.isEmpty()) return

            connection.prepareStatement("SELECT message_key FROM kentai.login_table WHERE user_uuid IN(${"?, ".repeat(req.requestedUUIDs.size).dropLast(2)})").use { statement1 ->
                for ((i, uuid) in req.requestedUUIDs.withIndex()) {
                    statement1.setString(i + 1, uuid.toString())
                }
                val keyMap = mutableMapOf<UUID, RSAPublicKey>()
                statement1.executeQuery().use { query ->
                    var i = 0
                    while (query.next()) {
                        keyMap[req.requestedUUIDs[i]] = query.getString("message_key").toKey() as RSAPublicKey
                        i++
                    }
                }
                gson.toJson(KeyResponse(keyMap), response.writer)
                baseRequest.isHandled = true
            }
        }
    }
}