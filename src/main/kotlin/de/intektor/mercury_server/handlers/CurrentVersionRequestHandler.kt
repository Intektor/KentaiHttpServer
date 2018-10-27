package de.intektor.mercury_server.handlers

import de.intektor.mercury_common.client_to_server.CurrentVersionRequest
import de.intektor.mercury_common.gson.genGson
import de.intektor.mercury_common.server_to_client.CurrentVersionResponse
import de.intektor.mercury_server.MercuryServer
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author Intektor
 */
class CurrentVersionRequestHandler : AbstractHandler() {

    override fun handle(target: String?, baseRequest: Request?, request: HttpServletRequest, response: HttpServletResponse) {
        response.contentType = "application/json; charset=utf-8"
        response.status = HttpServletResponse.SC_OK

        val reqGson = genGson()
        val req = reqGson.fromJson(request.reader, CurrentVersionRequest::class.java)

        val recentChanges = MercuryServer.changes.filter { it.versionCode > req.currentVersionCode }.sortedByDescending { it.versionCode }
        val resGson = genGson()
        resGson.toJson(CurrentVersionResponse(recentChanges), response.writer)

        baseRequest?.isHandled = true
    }
}