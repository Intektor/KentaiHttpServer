package de.intektor.mercury_server

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.SQLException
import java.util.logging.Level

/**
 * @author Intektor
 */
object DatabaseConnection {

    @Volatile
    lateinit var ds: HikariDataSource

    fun buildConnection(username: String, password: String) {
        val config = HikariConfig()
        config.idleTimeout = 60000
        config.connectionTimeout = 60000
        config.validationTimeout = 3000
        config.maxLifetime = 60000
        config.addDataSourceProperty("tcpKeepAlive", true)
        config.addDataSourceProperty("autoReconnect", true)
        config.jdbcUrl = "jdbc:mysql://localhost/mercury?user=$username&password=$password&useSSL=false"
        ds = HikariDataSource(config)
        try {
            ds.connection.use { connection ->
                connection.prepareCall("CREATE DATABASE IF NOT EXISTS mercury /*!40100 DEFAULT CHARACTER SET utf8 */;").execute()
                connection.prepareCall("CREATE TABLE IF NOT EXISTS mercury.login_table (" +
                        "username varchar(20) NOT NULL," +
                        "user_uuid varchar(40) NOT NULL," +
                        "message_key varchar(400) NOT NULL," +
                        "auth_key varchar(400) NOT NULL," +
                        "fcm_token varchar(400) NOT NULL," +
                        "PRIMARY KEY (user_uuid))"
                ).execute()

                connection.prepareStatement("CREATE TABLE IF NOT EXISTS mercury.pending_messages (" +
                        "message_uuid VARCHAR(40) NOT NULL," +
                        "aes_key VARCHAR(344), " +
                        "init_vector VARCHAR(344), " +
                        "time_sent BIGINT, " +
                        "signature BLOB, " +
                        "chat_uuid VARCHAR(344) NOT NULL, " +
                        "sender_uuid VARCHAR(40) NOT NULL, " +
                        "receiver_uuid VARCHAR(40) NOT NULL, " +
                        "data TEXT NOT NULL, " +
                        "PRIMARY KEY(message_uuid));").execute()

                connection.prepareStatement("CREATE TABLE IF NOT EXISTS mercury.references (" +
                        "reference_uuid VARCHAR(40) NOT NULL, " +
                        "state INT NOT NULL, " +
                        "times_tried INT NOT NULL, " +
                        "upload_time BIGINT NOT NULL, " +
                        "PRIMARY KEY(reference_uuid));").execute()

                connection.prepareStatement("CREATE TABLE IF NOT EXISTS mercury.fetch_history (" +
                        "id INT AUTO_INCREMENT, " +
                        "user_uuid VARCHAR(40) NOT NULL, " +
                        "last_message_time VARCHAR(40) NOT NULL, " +
                        "time BIGINT, " +
                        "PRIMARY KEY (id));").execute()

                connection.prepareStatement("CREATE TABLE IF NOT EXISTS mercury.user_status_table (" +
                        "user_uuid VARCHAR(40) NOT NULL, " +
                        "last_time_online BIGINT, " +
                        "type_closed INT, " +
                        "PRIMARY KEY(user_uuid));").execute()
            }
        } catch (e: SQLException) {
            MercuryServer.logger.log(Level.WARNING, "ERROR!", e)
        }
    }
}