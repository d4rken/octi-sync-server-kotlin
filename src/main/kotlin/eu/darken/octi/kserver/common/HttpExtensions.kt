package eu.darken.octi.kserver.common

import io.ktor.server.plugins.*
import io.ktor.server.routing.*

val RoutingContext.callInfo: String
    get() {
        val ipAddress = call.request.origin.remoteHost
        val userAgent = call.request.headers["User-Agent"]
        return "$ipAddress ($userAgent)"
    }