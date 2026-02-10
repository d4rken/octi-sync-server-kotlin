package eu.darken.octi.kserver.myip

import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MyIpRoute @Inject constructor() {

    fun setup(rootRoute: Routing) {
        rootRoute.get("/v1/myip") {
            val connectionIp = call.request.origin.remoteAddress
            val forwardedFor = call.request.headers["X-Forwarded-For"]
                ?.split(",")
                ?.firstOrNull()
                ?.trim()
                ?.takeIf { it.isValidIp() }
            val clientIp = forwardedFor ?: connectionIp
            call.respond(HttpStatusCode.OK, mapOf("ip" to clientIp))
        }
    }

    private fun String.isValidIp(): Boolean = try {
        InetAddress.getByName(this)
        true
    } catch (e: Exception) {
        false
    }
}
