package eu.darken.octi.kserver.common

import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.INFO
import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.VERBOSE
import eu.darken.octi.kserver.common.debug.logging.log
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import kotlinx.coroutines.*
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

data class RateLimitConfig(
    val limit: Int = 512,
    val resetTime: Duration = Duration.ofSeconds(60),
)

fun Application.installRateLimit(config: RateLimitConfig) {
    log(INFO) { "Rate limits are set to $config" }
    val requests = ConcurrentHashMap<String, Pair<Int, Instant>>()

    launch(Dispatchers.IO) {
        while (currentCoroutineContext().isActive) {
            log(VERBOSE) { "Checking for stale rate limit entries..." }
            val now = Instant.now()
            val staleEntries = requests.filterValues { (_, resetTime) -> now.isAfter(resetTime) }
            if (staleEntries.isNotEmpty()) {
                log { "Removing ${staleEntries.size} stale rate limit entries: $staleEntries" }
                staleEntries.keys.forEach { requests.remove(it) }
            }
            log(VERBOSE) { "Entries checked, now $requests" }
            delay(config.resetTime.toMillis() / 2)
        }
    }

    intercept(ApplicationCallPipeline.Plugins) {
        val clientIp = call.request.run {
            headers["X-Forwarded-For"]?.split(",")?.firstOrNull()?.trim() ?: origin.remoteAddress
        }
        val currentTime = Instant.now()

        val requestInfo = requests[clientIp] ?: Pair(0, currentTime.plusSeconds(config.resetTime.seconds))
        log(VERBOSE) { "Rate limits for current request: $requestInfo" }
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
}