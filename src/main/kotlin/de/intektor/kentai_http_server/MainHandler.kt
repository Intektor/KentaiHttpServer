package de.intektor.kentai_http_server

import de.intektor.kentai_http_common.client_to_server.*
import de.intektor.kentai_http_server.handlers.*
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author Intektor
 */
object MainHandler : AbstractHandler() {

    private val registry: HashMap<String, AbstractHandler> = hashMapOf()

    override fun handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
        var handler = registry[target.substring(1)]
        if (handler == null) {
            handler = EmptyHandler()
        }
        try {
            handler.handle(target, baseRequest, request, response)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        baseRequest.isHandled = true
    }

    private fun register(request: String, handler: AbstractHandler) {
        registry[request] = handler
    }

    init {
        register(RegisterRequestToServer.TARGET, RegisterRequestToServerHandler())
        register(CheckUsernameAvailableRequestToServer.TARGET, CheckUsernameAvailableRequestToServerHandler())
        register(AddContactRequest.TARGET, AddContactRequestHandler())
        register(SendChatMessageRequest.TARGET, SendChatMessageRequestHandler())
        register(UpdateFBCMTokenRequest.TARGET, UpdateFBCMTokenRequestHandler())
        register(FetchMessageRequest.TARGET, FetchMessageRequestHandler())
        register(KeyRequest.TARGET, KeyRequestHandler())
        register("uploadReference", UploadReferenceHandler())
        register(DownloadReferenceRequest.TARGET, DownloadReferenceHandler())
        register(CurrentVersionRequest.TARGET, CurrentVersionRequestHandler())
        register("uploadProfilePicture", UploadProfilePictureHandler())
    }
}