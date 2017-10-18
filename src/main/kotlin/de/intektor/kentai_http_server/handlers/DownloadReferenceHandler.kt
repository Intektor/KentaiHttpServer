package de.intektor.kentai_http_server.handlers

import de.intektor.kentai_http_common.client_to_server.DownloadReferenceRequest
import de.intektor.kentai_http_common.gson.genGson
import de.intektor.kentai_http_server.DatabaseConnection
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author Intektor
 */
class DownloadReferenceHandler : AbstractHandler() {

    override fun handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
        val reqGson = genGson()
        val req = reqGson.fromJson(request.reader, DownloadReferenceRequest::class.java)
        DatabaseConnection.ds.connection.use { connection ->
            connection.prepareStatement("SELECT state FROM kentai.references WHERE reference_uuid = ?").use { statement ->
                statement.setString(1, req.referenceUUID.toString())
                statement.executeQuery().use { query ->
                    if (query.next()) {
                        val state = UploadReferenceHandler.State.values()[query.getInt("state")]
                        when (state) {
                            UploadReferenceHandler.State.IN_PROGRESS -> writeResponse(response.outputStream, DownloadReferenceRequest.Response.IN_PROGRESS, 0)
                            UploadReferenceHandler.State.DELETED -> writeResponse(response.outputStream, DownloadReferenceRequest.Response.DELETED, 0)
                            UploadReferenceHandler.State.FINISHED -> {
                                val file = File("references/${req.referenceUUID}")
                                writeResponse(response.outputStream, DownloadReferenceRequest.Response.SUCCESS, file.length())
                                FileInputStream(file).use { fileIn ->
                                    val written = fileIn.copyTo(response.outputStream)
                                    println(written)
                                }
                            }
                        }
                    } else {
                        writeResponse(response.outputStream, DownloadReferenceRequest.Response.NOT_FOUND, 0)
                        baseRequest.isHandled = true
                    }
                }
            }
        }
    }

    private fun writeResponse(outputStream: OutputStream, response: DownloadReferenceRequest.Response, totalToSend: Long) {
        val dataOut = DataOutputStream(outputStream)
        dataOut.writeInt(response.ordinal)
        dataOut.writeLong(totalToSend)
    }
}