package de.intektor.kentai_http_server

import org.eclipse.jetty.server.Server
import java.io.FileInputStream
import java.util.*
import java.util.logging.*


/**
 * @author Intektor
 */
object KentaiServer {

    lateinit var logger: Logger

    fun startServer() {
        logger = Logger.getLogger("KentaiServer")
        try {
            val file = FileHandler("log.txt", true)
            val stream = StreamHandler(System.out, SimpleFormatter())
            logger.addHandler(stream)
            logger.addHandler(file)
        } catch (e: Exception) {
            logger.log(Level.WARNING, "EXCEPTION!", e)
        }

        val scanner = Scanner(FileInputStream("user.txt"))
        val username = scanner.next()
        val password = scanner.next()

        DatabaseConnection.buildConnection(username, password)

        scanner.close()

        val server = Server(17349)
        server.handler = MainHandler
        server.start()
        server.dumpStdErr()
        server.join()
    }
}