package eu.darken.octi.kserver.common.debug.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import org.slf4j.LoggerFactory
import kotlin.math.min

class ConsoleLogger : Logging.Logger {

    private val logger by lazy {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext

        loggerContext.getLogger("Octi").apply {
            level = Level.TRACE
        }
        LoggerFactory.getLogger("Octi")
    }

    override fun isLoggable(priority: Logging.Priority): Boolean = true

    override fun log(priority: Logging.Priority, tag: String, message: String, metaData: Map<String, Any>?) {
        var i = 0
        val length = message.length
        while (i < length) {
            var newline = message.indexOf('\n', i)
            newline = if (newline != -1) newline else length
            do {
                val end = min(newline, i + MAX_LOG_LENGTH)
                val part = message.substring(i, end)
                writeToLogcat(priority, tag, part)
                i = end
            } while (i < newline)
            i++
        }
    }

    private fun writeToLogcat(priority: Logging.Priority, tag: String, part: String) {
        val line = "$tag: $part"
        when (priority) {
            Logging.Priority.VERBOSE -> logger.trace(line)
            Logging.Priority.DEBUG -> logger.debug(line)
            Logging.Priority.INFO -> logger.info(line)
            Logging.Priority.WARN -> logger.warn(line)
            Logging.Priority.ERROR -> logger.error(line)
            Logging.Priority.ASSERT -> logger.error(line)
        }
    }

    companion object {
        private const val MAX_LOG_LENGTH = 4000
    }
}