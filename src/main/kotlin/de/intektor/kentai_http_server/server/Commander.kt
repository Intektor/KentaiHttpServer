package de.intektor.kentai_http_server.server

import com.google.common.base.Splitter
import de.intektor.kentai_http_server.KentaiServer
import de.intektor.kentai_http_server.server.commands.CommandReloadVersionManager
import de.intektor.kentai_http_server.server.commands.CommandSendNotification
import de.intektor.kentai_http_server.server.commands.CommandShutdownServer
import de.intektor.kentai_http_server.server.commands.ListUsersCommand
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
            KentaiServer.logger.info("Input received: $input")
            val brokenInput = splitter.splitToList(input)
            val command: Command? = registry[brokenInput[0]]
            if (command == null) {
                KentaiServer.logger.info("No command found like: ${brokenInput[0]}")
            } else {
                KentaiServer.logger.info("Executing command!")
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