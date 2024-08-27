package eu.darken.octi.kserver

import eu.darken.octi.kserver.common.AppScope
import eu.darken.octi.kserver.common.RateLimitConfig
import eu.darken.octi.kserver.common.debug.logging.ConsoleLogger
import eu.darken.octi.kserver.common.debug.logging.Logging
import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.INFO
import eu.darken.octi.kserver.common.debug.logging.log
import eu.darken.octi.kserver.common.debug.logging.logTag
import java.nio.file.Path
import java.time.Duration
import javax.inject.Inject
import kotlin.io.path.Path
import kotlin.io.path.absolute

class App @Inject constructor(
    private val config: Config,
    val appScope: AppScope,
    private val server: Server,
) {

    init {
        if (config.isDebug) {
            println("Debug mode enabled")
            if (Logging.loggers.none { it is ConsoleLogger }) {
                Logging.install(ConsoleLogger())
            }
            log(TAG, INFO) { "Debug mode is active" }
        }
        log(TAG, INFO) { "App config is $config" }
    }

    fun launch() {
        server.start()
    }

    fun isRunning(): Boolean {
        return server.isRunning()
    }

    fun shutdown() {
        server.stop()
    }

    data class Config(
        val isDebug: Boolean = false,
        val port: Int,
        val dataPath: Path,
        val rateLimit: RateLimitConfig? = RateLimitConfig(),
        val payloadLimit: Long? = 128 * 1024L,
        val accountGCInterval: Duration = Duration.ofMinutes(10),
        val shareExpiration: Duration = Duration.ofMinutes(60),
        val shareGCInterval: Duration = Duration.ofMinutes(10),
        val deviceExpiration: Duration = Duration.ofDays(90),
        val deviceGCInterval: Duration = Duration.ofMinutes(10),
        val moduleExpiration: Duration = Duration.ofDays(90),
        val moduleGCInterval: Duration = Duration.ofMinutes(10),
    )

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println("Program arguments: ${args.joinToString()}")

            val config = Config(
                isDebug = args.any { it.startsWith("--debug") },
                port = args
                    .singleOrNull { it.startsWith("--port") }
                    ?.substringAfter('=')?.toInt()
                    ?: 8080,
                dataPath = args
                    .single { it.startsWith("--datapath") }
                    .let { Path(it.substringAfter('=')) }
                    .absolute(),
                rateLimit = if (args.any { it.startsWith("--disable-rate-limits") }) {
                    null
                } else {
                    RateLimitConfig()
                },
            )

            if (config.isDebug) {
                println("Debug mode enabled")
                if (Logging.loggers.none { it is ConsoleLogger }) {
                    Logging.install(ConsoleLogger())
                }
                log(TAG, INFO) { "Debug mode is active" }
            }

            DaggerAppComponent.builder().config(config).build().application().apply {
                launch()
            }
        }

        private val TAG = logTag("App")
    }
}