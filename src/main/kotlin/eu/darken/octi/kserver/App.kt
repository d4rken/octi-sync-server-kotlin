package eu.darken.octi.kserver

import eu.darken.octi.kserver.account.AccountRepo
import eu.darken.octi.kserver.account.share.ShareRepo
import eu.darken.octi.kserver.common.AppScope
import eu.darken.octi.kserver.common.debug.logging.ConsoleLogger
import eu.darken.octi.kserver.common.debug.logging.Logging
import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.INFO
import eu.darken.octi.kserver.common.debug.logging.log
import eu.darken.octi.kserver.common.debug.logging.logTag
import eu.darken.octi.kserver.device.DeviceRepo
import eu.darken.octi.kserver.module.ModuleRepo
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.Path

class App @Inject constructor(
    private val config: Config,
    val appScope: AppScope,
    private val server: Server,
    val accountRepo: AccountRepo,
    val shareRepo: ShareRepo,
    val deviceRepo: DeviceRepo,
    val moduleRepo: ModuleRepo,
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
        val isDebug: Boolean,
        val port: Int,
        val dataPath: Path,
        val useRateLimit: Boolean = true,
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
                    .let { Path(it.substringAfter('=')) },
            )

            DaggerAppComponent.builder().config(config).build().application().apply {
                launch()
            }
        }

        private val TAG = logTag("App")
    }
}