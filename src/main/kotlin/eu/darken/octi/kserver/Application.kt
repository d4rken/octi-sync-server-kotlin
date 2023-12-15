package eu.darken.octi.kserver

import eu.darken.octi.kserver.common.AppScope
import javax.inject.Inject

class Application @Inject constructor(
    private val appScope: AppScope,
    private val router: Router
) {

    fun launch() {
        router.start()
    }

    companion object {
        var isDebug = false
        var deploymentType = "development"

        @JvmStatic
        fun main(args: Array<String>) {
            println("Hello World!")
            println("Program arguments: ${args.joinToString()}")
            DaggerAppComponent.builder().build().application().launch()
        }
    }
}