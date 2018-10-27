package de.intektor.mercury_server

import com.google.common.hash.Hashing
import java.io.File
import java.math.BigInteger
import java.util.*

/**
 * @author Intektor
 */
fun sha256(input: String): String {
    val digest = Hashing.sha256().hashBytes(input.toByteArray()).asBytes()
    return String.format("%064x", BigInteger(1, digest))
}

fun hasProfilePictureBeenUpdated(userUUID: UUID, lastTimeUpdated: Long): Boolean {
    val file = File("pp/$userUUID.png")
    return file.exists() && file.lastModified() > lastTimeUpdated
}