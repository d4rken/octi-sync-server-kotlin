package eu.darken.octi.kserver.common.debug.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.encoder.LayoutWrappingEncoder
import org.slf4j.LoggerFactory
import kotlin.math.min

object ConsoleLogger : Logging.Logger {
    private const val MAX_LOG_LENGTH = 4000
    private val logger by lazy {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext

        loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).apply {
            detachAndStopAllAppenders()
        }

        val patternLayout = PatternLayout().apply {
            context = loggerContext
            pattern = "%d{HH:mm:ss.SSS} %-5level %msg%n"
            start()
        }

        val consoleAppender = ConsoleAppender<ILoggingEvent>().apply {
            context = loggerContext
            encoder = LayoutWrappingEncoder<ILoggingEvent>().apply {
                layout = patternLayout
            }
            start()
        }

        loggerContext.getLogger("Octi").apply {
            level = Level.TRACE
            addAppender(consoleAppender)
        }
        LoggerFactory.getLogger("Octi")
    }
    var logLevel = Logging.Priority.INFO

    override fun isLoggable(priority: Logging.Priority): Boolean = priority.code >= logLevel.code

    override fun log(priority: Logging.Priority, tag: String, message: String, metaData: Map<String, Any>?) {
        var i = 0
        val length = message.length
        while (i < length) {
            var newline = message.indexOf('\n', i)
            newline = if (newline != -1) newline else length
            do {
                val end = min(newline, i + MAX_LOG_LENGTH)
                val part = message.substring(i, end)
                writeToLog(priority, tag, part)
                i = end
            } while (i < newline)
            i++
        }
    }

    private fun writeToLog(priority: Logging.Priority, tag: String, part: String) {
        val line = "$tag - $part"
        when (priority) {
            Logging.Priority.VERBOSE -> logger.trace(line)
            Logging.Priority.DEBUG -> logger.debug(line)
            Logging.Priority.INFO -> logger.info(line)
            Logging.Priority.WARN -> logger.warn(line)
            Logging.Priority.ERROR -> logger.error(line)
            Logging.Priority.ASSERT -> logger.error(line)
        }
    }


}