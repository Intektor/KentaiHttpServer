package de.intektor.mercury_server.server.commands

import de.intektor.mercury_server.DatabaseConnection
import de.intektor.mercury_server.MercuryServer
import de.intektor.mercury_server.server.Command

/**
 * @author Intektor
 */
class ListUsersCommand : Command {

    override fun execute(name: String, args: List<String>) {
        DatabaseConnection.ds.connection.use { connection ->
            connection.prepareStatement("SELECT username, user_uuid FROM mercury.login_table;").executeQuery().use { query ->
                while (query.next()) {
                    println(query.getString("username") + " |-| " + query.getString("user_uuid"))
                }
            }
        }
    }

    override fun getNames(): List<String> = listOf("listUsers", "users")

    override fun logHelp() {
        MercuryServer.logger.info("Lists all users known to the mercury server")
    }

}