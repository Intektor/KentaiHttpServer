package de.intektor.kentai_http_server.handlers

import com.google.gson.stream.JsonWriter
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author Intektor
 */
class EmptyHandler : AbstractHandler() {
    override fun handle(target: String?, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
        val writer = JsonWriter(response.writer)
        writer.setIndent("  ")
        writer.beginObject()
        writer.name("debug").value("Server is online and ready!")
        writer.endObject()
        baseRequest.isHandled = true
    }

}