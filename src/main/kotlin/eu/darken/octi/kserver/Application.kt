package eu.darken.octi.kserver

import eu.darken.octi.kserver.common.AppScope
import eu.darken.octi.kserver.common.debug.logging.ConsoleLogger
import eu.darken.octi.kserver.common.debug.logging.Logging
import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.INFO
import eu.darken.octi.kserver.common.debug.logging.log
import eu.darken.octi.kserver.common.debug.logging.logTag
import javax.inject.Inject

class Application @Inject constructor(
    private val appScope: AppScope,
    private val router: Router
) {

    fun launch() {
        router.start()
    }

    companion object {
        var isDebug = true

        @JvmStatic
        fun main(args: Array<String>) {
            println("Hello World!")
            println("Program arguments: ${args.joinToString()}")
            if (isDebug) {
                println("Debug mode enabled")
                Logging.install(ConsoleLogger())
                log(TAG, INFO) { "Debug mode is active" }
            }
            DaggerAppComponent.builder().build().application().launch()
        }

        private val TAG = logTag("App")
    }
}