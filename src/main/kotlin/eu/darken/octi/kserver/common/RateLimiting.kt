package eu.darken.octi.kserver.common

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

val requests = ConcurrentHashMap<String, Pair<Int, Instant>>()

data class RateLimitConfig(
    val limit: Int = 20,
    val resetTime: Duration = Duration.ofSeconds(20),
    val maxBodySize: Long = 128 * 1024L,
)

fun Application.installRateLimit(config: RateLimitConfig = RateLimitConfig()) {
    intercept(ApplicationCallPipeline.Plugins) {
        val clientIp = call.request.origin.remoteHost
        val currentTime = Instant.now()

        val requestInfo = requests[clientIp] ?: Pair(0, currentTime.plusSeconds(config.resetTime.seconds))
        val requestCount = requestInfo.first
        val resetTime = requestInfo.second

        if (currentTime.isAfter(resetTime)) {
            requests[clientIp] = Pair(1, currentTime.plusSeconds(config.resetTime.seconds))
        } else if (requestCount >= config.limit) {
            call.respond(HttpStatusCode.TooManyRequests, "Rate limit exceeded. Try again later.")
            finish()
            return@intercept
        } else {
            requests[clientIp] = Pair(requestCount + 1, resetTime)
        }
    }
    intercept(ApplicationCallPipeline.Plugins) {
        if (call.request.receiveChannel().availableForRead > config.maxBodySize) {
            call.respond(HttpStatusCode.PayloadTooLarge, "Request body is too large")
            finish()
        }
    }
}