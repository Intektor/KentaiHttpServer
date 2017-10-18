package de.intektor.kentai_http_server.handlers

import de.intektor.kentai_http_common.reference.UploadResponse
import de.intektor.kentai_http_server.DatabaseConnection
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import java.io.*
import java.sql.Connection
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author Intektor
 */
class UploadReferenceHandler : AbstractHandler() {

    override fun handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
        val dataIn = DataInputStream(request.inputStream)
        val referenceUUID = dataIn.readUTF()
        DatabaseConnection.ds.connection.use { connection ->
            connection.prepareStatement("SELECT state, times_tried FROM kentai.references WHERE reference_uuid = ?").use { statement ->
                statement.setString(1, referenceUUID)
                statement.executeQuery().use { query ->
                    if (query.next()) {
                        val state = State.values()[query.getInt(1)]
                        if (state == State.DELETED || state == State.FINISHED) {
                            DataOutputStream(response.outputStream).use { dataOut ->
                                dataOut.writeInt(UploadResponse.ALREADY_UPLOADED.ordinal)
                            }
                            baseRequest.isHandled = true
                            return
                        }

                        val timesTried = query.getInt(2)
                        receiveData(request.inputStream, timesTried, connection, referenceUUID)
                        DataOutputStream(response.outputStream).use { dataOut ->
                            dataOut.writeInt(UploadResponse.NOW_UPLOADED.ordinal)
                        }
                        baseRequest.isHandled = true
                    } else {
                        connection.prepareStatement("INSERT INTO kentai.references (reference_uuid, state, times_tried, upload_time) VALUES(?, ?, 1, ?)").use { statement2 ->
                            statement2.setString(1, referenceUUID)
                            statement2.setInt(2, State.IN_PROGRESS.ordinal)
                            statement2.setLong(3, System.currentTimeMillis())
                            statement2.execute()
                        }
                        receiveData(request.inputStream, 1, connection, referenceUUID)
                        DataOutputStream(response.outputStream).use { dataOut ->
                            dataOut.writeInt(UploadResponse.NOW_UPLOADED.ordinal)
                        }
                        baseRequest.isHandled = true
                    }
                }
            }
        }
    }

    private fun receiveData(inputStream: InputStream, timesTried: Int, connection: Connection, referenceUUID: String) {
        try {
            File("references").mkdirs()
            FileOutputStream("references/$referenceUUID").use { fileOut ->
                inputStream.copyTo(fileOut)
            }
            connection.prepareStatement("UPDATE kentai.references SET state = ? WHERE reference_uuid = ?").use { statement ->
                statement.setInt(1, State.FINISHED.ordinal)
                statement.setString(2, referenceUUID)
                statement.execute()
            }
        } catch (t: Throwable) {
            connection.prepareStatement("UPDATE kentai.references SET times_tried = times_tried + 1 WHERE reference_uuid = ?").use { statement ->
                statement.setString(1, referenceUUID)
                statement.execute()
            }
            if (timesTried > 10) {
                connection.prepareStatement("UPDATE kentai.references SET state = ? WHERE reference_uuid = ?").use { statement ->
                    statement.setInt(1, State.DELETED.ordinal)
                    statement.setString(2, referenceUUID)
                    statement.execute()
                }
            }
        }
    }

    enum class State {
        IN_PROGRESS,
        FINISHED,
        DELETED
    }
}
