package de.intektor.mercury_server.util

import de.intektor.mercury_common.util.toUUID
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.*

/**
 * @author Intektor
 */
fun PreparedStatement.setUUID(column: Int, item: UUID) {
    setString(column, item.toString())
}

fun ResultSet.getUUID(column: Int): UUID = getString(column).toUUID()