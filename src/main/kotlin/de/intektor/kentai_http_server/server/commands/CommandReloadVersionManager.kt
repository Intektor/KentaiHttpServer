package de.intektor.kentai_http_server.server.commands

import de.intektor.kentai_http_server.KentaiServer
import de.intektor.kentai_http_server.server.Command

/**
 * @author Intektor
 */
class CommandReloadVersionManager : Command {

    override fun execute(name: String, args: List<String>) {
        KentaiServer.reloadVersionManager()
    }

    override fun getNames(): List<String> = listOf("reloadVersionManager")

    override fun logHelp() {
        KentaiServer.logger.info("Reloads the client versions in the folder!")
    }

}