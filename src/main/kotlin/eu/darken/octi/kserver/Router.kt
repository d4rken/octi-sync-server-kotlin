package eu.darken.octi.kserver

import eu.darken.octi.kserver.account.AccountRoute
import eu.darken.octi.kserver.account.share.ShareRoute
import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.INFO
import eu.darken.octi.kserver.common.debug.logging.log
import eu.darken.octi.kserver.common.debug.logging.logTag
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
import kotlinx.serialization.modules.SerializersModule
import java.util.*
import javax.inject.Inject

class Router @Inject constructor(
    private val statusRoute: StatusRoute,
    private val authRoute: AccountRoute,
    private val shareRoute: ShareRoute,
    private val serializers: SerializersModule,
) {

    @Suppress("ExtractKtorModule")
    private val server by lazy {
        embeddedServer(Netty, 8080) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    serializersModule = serializers
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
            shareRoute.setup(this)
        }
    }

    fun start() {
        log(TAG, INFO) { "Router is starting..." }
        server.start(wait = true)
    }

    companion object {
        private val TAG = logTag("Router")
    }
}