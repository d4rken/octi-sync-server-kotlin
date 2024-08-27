package eu.darken.octi.kserver

import eu.darken.octi.kserver.account.AccountRoute
import eu.darken.octi.kserver.account.share.ShareRoute
import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.INFO
import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.WARN
import eu.darken.octi.kserver.common.debug.logging.log
import eu.darken.octi.kserver.common.debug.logging.logTag
import eu.darken.octi.kserver.common.installCallLogging
import eu.darken.octi.kserver.common.installPayloadLimit
import eu.darken.octi.kserver.common.installRateLimit
import eu.darken.octi.kserver.device.DeviceRoute
import eu.darken.octi.kserver.module.ModuleRoute
import eu.darken.octi.kserver.status.StatusRoute
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import javax.inject.Inject

class Server @Inject constructor(
    private val config: App.Config,
    private val statusRoute: StatusRoute,
    private val accountRoute: AccountRoute,
    private val shareRoute: ShareRoute,
    private val deviceRoute: DeviceRoute,
    private val moduleRoute: ModuleRoute,
    private val serializers: SerializersModule,
) {

    @Suppress("ExtractKtorModule")
    private val server by lazy {
        embeddedServer(Netty, config.port) {
            installCallLogging()
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    serializersModule = serializers
                })
            }

            config.rateLimit
                ?.let { installRateLimit(it) }
                ?: log(TAG, WARN) { "rateLimit is not configured" }

            config.payloadLimit
                ?.let { installPayloadLimit(it) }
                ?: log(TAG, WARN) { "payloadLimit is not configured" }

            routing {
                statusRoute.setup(this)
                accountRoute.setup(this)
                shareRoute.setup(this)
                deviceRoute.setup(this)
                moduleRoute.setup(this)
            }
        }
    }
    private var isRunning = false

    fun start() {
        log(TAG, INFO) { "Server is starting..." }
        server.monitor.apply {
            subscribe(ApplicationStarted) {
                log(TAG, INFO) { "Server is ready" }
                isRunning = true
            }
            subscribe(ApplicationStopping) {
                log(TAG, INFO) { "Server is stopping..." }
                isRunning = false
            }
        }
        server.start(wait = true)
    }

    fun stop() {
        log(TAG, INFO) { "Server is stopping..." }
        server.stop(gracePeriodMillis = 1000, timeoutMillis = 5000)
        log(TAG, INFO) { "Server stopped" }
    }

    fun isRunning(): Boolean = isRunning

    companion object {
        private val TAG = logTag("Server")
    }
}