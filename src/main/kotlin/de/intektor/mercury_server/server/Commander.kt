package de.intektor.mercury_server.server

import com.google.common.base.Splitter
import de.intektor.mercury_server.MercuryServer
import de.intektor.mercury_server.server.commands.CommandReloadVersionManager
import de.intektor.mercury_server.server.commands.CommandSendNotification
import de.intektor.mercury_server.server.commands.CommandShutdownServer
import de.intektor.mercury_server.server.commands.ListUsersCommand
import java.util.*
import kotlin.collections.HashMap

/**
 * @author Intektor
 */
object Commander : Thread() {

    private val registry = HashMap<String, Command>()

    init {
        registerCommand(CommandShutdownServer())
        registerCommand(CommandSendNotification())
        registerCommand(CommandReloadVersionManager())
        registerCommand(ListUsersCommand())
    }

    override fun run() {
        val scanner = Scanner(System.`in`)
        val splitter = Splitter.on(' ')
        while (true) {
            val input = scanner.nextLine()
            if (input.isEmpty()) {
                continue
            }
            MercuryServer.logger.info("Input received: $input")
            val brokenInput = splitter.splitToList(input)
            val command: Command? = registry[brokenInput[0]]
            if (command == null) {
                MercuryServer.logger.info("No command found like: ${brokenInput[0]}")
            } else {
                MercuryServer.logger.info("Executing command!")
                command.execute(brokenInput[0], brokenInput.subList(1, brokenInput.size))
            }

        }
    }

    private fun registerCommand(command: Command) {
        for (name in command.getNames()) {
            registry.put(name, command)
        }
    }
}