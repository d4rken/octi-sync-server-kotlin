package eu.darken.octi.kserver

import eu.darken.octi.kserver.account.AccountRoute
import eu.darken.octi.kserver.status.StatusRoute
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.application.Application
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import java.util.*
import javax.inject.Inject

class Router @Inject constructor(
    private val statusRoute: StatusRoute,
    private val authRoute: AccountRoute,
) {

    @Suppress("ExtractKtorModule")
    private val server by lazy {
        embeddedServer(Netty, 8080) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                })
            }
            extracted()
        }
    }

    private fun Application.extracted() {
        routing {
            get("/v1") {
                call.respondText("ello  ${UUID.randomUUID()}", ContentType.Text.Html)
            }
            statusRoute.setup(this)
            authRoute.setup(this)
        }
    }

    fun start() {
        server.start(wait = true)
    }
}