package eu.darken.octi.kserver.common

import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.VERBOSE
import eu.darken.octi.kserver.common.debug.logging.log
import io.ktor.server.application.*
import io.ktor.server.application.ApplicationCallPipeline.ApplicationPhase.Plugins
import io.ktor.server.request.*

fun Application.installCallLogging() {
    intercept(Plugins) {
        val method = call.request.httpMethod.value
        val uri = call.request.uri
        val userAgent = call.request.userAgent() ?: "Unknown"
        val ip = call.request.header("X-Forwarded-For") ?: call.request.local.remoteHost
        log("HTTP", VERBOSE) { "$ip($userAgent): $method - $uri" }
    }
}