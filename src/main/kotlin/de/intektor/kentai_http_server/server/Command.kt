package de.intektor.kentai_http_server.server

/**
 * @author Intektor
 */
interface Command {

    fun execute(name: String, args: List<String>)

    fun getNames(): List<String>

    fun logHelp()
}