package de.intektor.mercury_server.handlers

import de.intektor.mercury_common.client_to_server.DownloadProfilePictureRequest
import de.intektor.mercury_common.gson.genGson
import de.intektor.mercury_common.users.ProfilePictureType
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import org.imgscalr.Scalr
import java.io.DataOutputStream
import java.io.File
import javax.imageio.ImageIO
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author Intektor
 */
class DownloadProfilePictureRequestHandler : AbstractHandler() {

    override fun handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
        val reqGson = genGson()
        val req = reqGson.fromJson(request.reader, DownloadProfilePictureRequest::class.java)
        val loaded = ImageIO.read(File("pp", "${req.userUUID}.png"))
        val using = when (req.type) {
            ProfilePictureType.SMALL -> {
                Scalr.resize(loaded, Scalr.Method.BALANCED, Scalr.Mode.FIT_EXACT, 40, 40)
            }
            ProfilePictureType.NORMAL -> loaded
        }

        ImageIO.write(using, "PNG", DataOutputStream(response.outputStream))

        baseRequest.isHandled = true
    }
}