package eu.darken.octi.kserver.common

import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.*
import eu.darken.octi.kserver.common.debug.logging.log
import eu.darken.octi.kserver.common.debug.logging.logTag
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.*
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

data class RateLimitConfig(
    val limit: Int = 512,
    val resetTime: Duration = Duration.ofSeconds(60),
)

private val TAG = logTag("RateLimiter")

private data class ClientRateState(
    val id: String,
    val requests: Int = 0,
    val resetAt: Instant,
)

fun Application.installRateLimit(config: RateLimitConfig) {
    log(TAG, INFO) { "Rate limits are set to $config" }
    val rateLimitCache = ConcurrentHashMap<String, ClientRateState>()

    launch(Dispatchers.IO) {
        while (currentCoroutineContext().isActive) {
            log(TAG) { "Checking for stale rate limit entries (${rateLimitCache.size} entries)..." }
            val top10 = rateLimitCache.values.sortedByDescending { it.requests }.take(10)
            log(TAG, VERBOSE) { "Rate limit top 10 by requests: $top10" }
            val now = Instant.now()
            val staleEntries = rateLimitCache.filterValues { state -> now.isAfter(state.resetAt) }
            if (staleEntries.isNotEmpty()) {
                log(TAG) { "Removing ${staleEntries.size} stale rate limit entries: $staleEntries" }
                staleEntries.keys.forEach { rateLimitCache.remove(it) }
            }
            delay(config.resetTime.toMillis() / 2)
        }
    }

    intercept(ApplicationCallPipeline.Plugins) {
        val clientIp = call.request.run {
            headers["X-Forwarded-For"]?.split(",")?.firstOrNull()?.trim() ?: origin.remoteAddress
        }
        val now = Instant.now()
        val calldetails = "${call.request.httpMethod.value} ${call.request.uri}"

        val rateState = rateLimitCache[clientIp] ?: ClientRateState(id = clientIp, resetAt = now + config.resetTime)

        if (now.isAfter(rateState.resetAt)) {
            rateLimitCache[clientIp] = ClientRateState(clientIp, 1, now + config.resetTime)
        } else if (rateState.requests >= config.limit) {
            log(TAG, WARN) { "Rate limits exceeded by $rateState -- $calldetails" }
            call.respond(HttpStatusCode.TooManyRequests, "Rate limit exceeded. Try again later.")
            finish()
            return@intercept
        } else {
            rateLimitCache[clientIp] = rateState.copy(requests = rateState.requests + 1)
        }
    }
}