package de.intektor.mercury_server.util

/**
 * @author Intektor
 */
object Logger {

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        System.err.println("ERROR: $tag: $message${if (throwable != null) " | ${throwable.localizedMessage}" else ""}")
    }

    fun info(tag: String, message: String, throwable: Throwable? = null) {
        System.out.println("INFO: $tag: $message${if (throwable != null) " | ${throwable.localizedMessage}" else ""}")
    }
}