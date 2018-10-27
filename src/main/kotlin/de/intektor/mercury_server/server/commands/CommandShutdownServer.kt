package de.intektor.mercury_server.server.commands

import de.intektor.mercury_server.server.Command

/**
 * @author Intektor
 */
class CommandShutdownServer : Command {

    override fun execute(name: String, args: List<String>) {
        System.exit(0)
    }

    override fun getNames(): List<String> = listOf("shutdown", "quit", "stop")

    override fun logHelp() {
    }
}