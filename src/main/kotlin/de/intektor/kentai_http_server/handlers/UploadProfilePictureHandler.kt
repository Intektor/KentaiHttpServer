package de.intektor.kentai_http_server.handlers

import de.intektor.kentai_http_common.gson.genGson
import de.intektor.kentai_http_common.server_to_client.UploadProfilePictureResponse
import de.intektor.kentai_http_common.tcp.sendPacket
import de.intektor.kentai_http_common.tcp.server_to_client.ProfilePictureUpdatedPacketToClient
import de.intektor.kentai_http_common.util.readUUID
import de.intektor.kentai_http_common.util.toKey
import de.intektor.kentai_http_server.DatabaseConnection
import de.intektor.kentai_http_server.DirectConnector
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.Socket
import java.security.PublicKey
import java.security.Signature
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


/**
 * @author Intektor
 */
class UploadProfilePictureHandler : AbstractHandler() {

    override fun handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
        val dataIn = DataInputStream(request.inputStream)
        val gson = genGson()

        val userUUID = dataIn.readUUID()
        DatabaseConnection.ds.connection.use { connection ->
            val authKey = connection.prepareStatement("SELECT auth_key FROM kentai.login_table WHERE user_uuid = ?").use { statement ->
                statement.setString(1, userUUID.toString())
                statement.executeQuery().use { query ->
                    if (query.next()) {
                        query.getString("auth_key").toKey() as PublicKey
                    } else {
                        val r = UploadProfilePictureResponse(UploadProfilePictureResponse.Type.FAILED_UNKNOWN_USER)
                        gson.toJson(r, response.writer)
                        return
                    }
                }
            }

            val signedAmount = dataIn.readInt()
            val signed = ByteArray(signedAmount)
            dataIn.readFully(signed)

            val pictureAmount = dataIn.readInt()
            val picture = ByteArray(pictureAmount)
            dataIn.readFully(picture)

            val signer = Signature.getInstance("SHA1WithRSA")
            signer.initVerify(authKey)
            signer.update(picture)
            if (!signer.verify(signed)) {
                val r = UploadProfilePictureResponse(UploadProfilePictureResponse.Type.FAILED_VERIFY_SIGNATURE)
                gson.toJson(r, response.writer)
                return
            }

            File("pp/").mkdirs()
            File("pp/$userUUID.png").outputStream().use { output ->
                output.write(picture)
            }

            val r = UploadProfilePictureResponse(UploadProfilePictureResponse.Type.SUCCESS)
            gson.toJson(r, response.writer)

            for (socket in DirectConnector.interestedMap[userUUID] ?: emptyList<Socket>()) {
                sendPacket(ProfilePictureUpdatedPacketToClient(userUUID), DataOutputStream(socket.getOutputStream()))
            }
        }

        baseRequest.isHandled = true
    }

}