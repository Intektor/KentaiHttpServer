package de.intektor.mercury_server.server.commands

import de.intektor.mercury_server.MercuryServer
import de.intektor.mercury_server.server.Command

/**
 * @author Intektor
 */
class CommandReloadVersionManager : Command {

    override fun execute(name: String, args: List<String>) {
        MercuryServer.reloadVersionManager()
    }

    override fun getNames(): List<String> = listOf("reloadVersionManager")

    override fun logHelp() {
        MercuryServer.logger.info("Reloads the client versions in the folder!")
    }

}