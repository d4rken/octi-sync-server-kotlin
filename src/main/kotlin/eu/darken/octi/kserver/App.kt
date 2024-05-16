package eu.darken.octi.kserver

import eu.darken.octi.kserver.common.AppScope
import eu.darken.octi.kserver.common.debug.logging.ConsoleLogger
import eu.darken.octi.kserver.common.debug.logging.Logging
import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.INFO
import eu.darken.octi.kserver.common.debug.logging.log
import eu.darken.octi.kserver.common.debug.logging.logTag
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.Path

class App @Inject constructor(
    val appScope: AppScope,
    private val router: Router,
) {

    fun launch() {
        router.start()
    }

    companion object {
        var isDebug = false
        lateinit var dataPath: Path

        @JvmStatic
        fun main(args: Array<String>) {
            println("Program arguments: ${args.joinToString()}")

            args.forEach {
                when {
                    it.startsWith("--debug") -> isDebug = true
                    it.startsWith("--datapath=") -> dataPath = Path(it.substringAfter('='))
                }
            }

            if (isDebug) {
                println("Debug mode enabled")
                Logging.install(ConsoleLogger())
                log(TAG, INFO) { "Debug mode is active" }
            }

            log(TAG, INFO) { "Data path is $dataPath" }

            val comp = DaggerAppComponent.builder().build()
            val app = comp.application()
            app.launch()
        }

        private val TAG = logTag("App")
    }
}