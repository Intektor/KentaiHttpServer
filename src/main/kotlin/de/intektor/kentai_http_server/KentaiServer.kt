package de.intektor.kentai_http_server

import com.google.common.base.Splitter
import de.intektor.kentai_http_common.server_to_client.CurrentVersionResponse
import de.intektor.kentai_http_server.server.Commander
import okhttp3.OkHttpClient
import org.eclipse.jetty.server.Server
import java.io.File
import java.io.FileInputStream
import java.util.*
import java.util.logging.*


/**
 * @author Intektor
 */
object KentaiServer {

    lateinit var logger: Logger

    val httpClient = OkHttpClient()

    val changes: MutableList<CurrentVersionResponse.ChangeLog> = mutableListOf()

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

        Commander.start()
        DirectConnector.start()

        reloadVersionManager()

        val server = Server(17349)
        server.handler = MainHandler
        server.start()
        server.dumpStdErr()
        server.join()
    }

    fun reloadVersionManager() {
        changes.clear()

        val splitter = Splitter.on('&')

        if (!File("changes/").exists()) return

        for (changeFile in File("changes/").listFiles()) {
            val scanner = Scanner(changeFile)
            val versionCode = scanner.nextLong()
            val versionName = scanner.next()
            val downloadLink = scanner.next()
            scanner.nextLine()
            val changeList = mutableListOf<CurrentVersionResponse.Change>()
            while (scanner.hasNextLine()) {
                val line = scanner.nextLine()
                val list = splitter.splitToList(line)
                changeList.add(CurrentVersionResponse.Change(list[1], when (list.first()) {
                    "A" -> CurrentVersionResponse.ChangeType.ADDITION
                    "R" -> CurrentVersionResponse.ChangeType.REMOVAL
                    "T" -> CurrentVersionResponse.ChangeType.TWEAK
                    "F" -> CurrentVersionResponse.ChangeType.FIX
                    else -> throw RuntimeException("File: ${changeFile.name} is corrupt! Could not find enum for letter ${list.first()}")
                }))
            }
            changes.add(CurrentVersionResponse.ChangeLog(versionCode, versionName, changeList, downloadLink))
        }
    }
}