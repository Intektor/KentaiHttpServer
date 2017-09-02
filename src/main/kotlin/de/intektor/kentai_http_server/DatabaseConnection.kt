package de.intektor.kentai_http_server

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.SQLException
import java.util.logging.Level

/**
 * @author Intektor
 */
object DatabaseConnection {
    @Volatile lateinit var ds: HikariDataSource

    fun buildConnection(username: String, password: String) {
        val config = HikariConfig()
        config.idleTimeout = 60000
        config.connectionTimeout = 60000
        config.validationTimeout = 3000
        config.maxLifetime = 60000
        config.jdbcUrl = "jdbc:mysql://localhost/kentai?user=$username&password=$password&useSSL=false"
        ds = HikariDataSource(config)
        try {
            ds.connection.use({ connection ->
                connection.prepareCall("CREATE DATABASE IF NOT EXISTS kentai /*!40100 DEFAULT CHARACTER SET utf8 */;").execute()
                connection.prepareCall("CREATE TABLE IF NOT EXISTS kentai.login_table (" +
                        "username varchar(20) NOT NULL," +
                        "user_uuid varchar(40) NOT NULL," +
                        "message_key varchar(400) NOT NULL," +
                        "auth_key varchar(400) NOT NULL," +
                        "fcm_token varchar(400) NOT NULL," +
                        "PRIMARY KEY (user_uuid))"
                ).execute()

                connection.prepareStatement("CREATE TABLE IF NOT EXISTS kentai.pending_messages (" +
                        "message_uuid VARCHAR(40) NOT NULL," +
                        "text VARCHAR(2000) NOT NULL, " +
                        "reference VARCHAR(150), " +
                        "registry_id VARCHAR(344), " +
                        "aes_key VARCHAR(344), " +
                        "init_vector VARCHAR(344), " +
                        "time_sent BIGINT, " +
                        "PRIMARY KEY(message_uuid));").execute()
            })
        } catch (e: SQLException) {
            KentaiServer.logger.log(Level.WARNING, "ERROR!", e)
        }
    }
}