package eu.darken.octi.kserver.myip

import eu.darken.octi.kserver.common.IpHelper
import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MyIpRoute @Inject constructor() {

    fun setup(rootRoute: Routing) {
        rootRoute.get("/v1/myip") {
            val connectionIp = call.request.origin.remoteAddress
            val clientIp = if (IpHelper.isLoopback(connectionIp)) {
                // Request is from a local reverse proxy, trust X-Forwarded-For
                call.request.headers["X-Forwarded-For"]
                    ?.split(",")
                    ?.firstOrNull()
                    ?.trim()
                    ?.takeIf { IpHelper.isValid(it) }
                    ?: connectionIp
            } else {
                connectionIp
            }
            call.respond(HttpStatusCode.OK, mapOf("ip" to clientIp))
        }
    }
}
