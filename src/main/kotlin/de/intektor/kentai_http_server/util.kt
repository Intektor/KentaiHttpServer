package de.intektor.kentai_http_server

import com.google.common.hash.Hashing
import java.math.BigInteger

/**
 * @author Intektor
 */
fun sha256(input: String): String {
    val digest = Hashing.sha256().hashBytes(input.toByteArray()).asBytes()
    return String.format("%064x", BigInteger(1, digest))
}