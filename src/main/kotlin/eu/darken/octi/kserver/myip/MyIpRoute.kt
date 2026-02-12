package eu.darken.octi.kserver.myip

import eu.darken.octi.kserver.common.clientIp
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MyIpRoute @Inject constructor() {

    fun setup(rootRoute: Routing) {
        rootRoute.get("/v1/myip") {
            val clientIp = call.request.clientIp()
            call.respond(HttpStatusCode.OK, mapOf("ip" to clientIp))
        }
    }
}
