package eu.darken.octi.kserver

import eu.darken.octi.kserver.status.StatusRoute
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*
import javax.inject.Inject

class Router @Inject constructor(
    private val statusRoute: StatusRoute,
) {

    private val server by lazy {
        embeddedServer(Netty, 8080) {
            routing {
                get("/v1") {
                    call.respondText("ello  ${UUID.randomUUID()}", ContentType.Text.Html)
                }
                statusRoute.setup(this)
            }
        }
    }

    fun start() {
        server.start(wait = true)
    }
}