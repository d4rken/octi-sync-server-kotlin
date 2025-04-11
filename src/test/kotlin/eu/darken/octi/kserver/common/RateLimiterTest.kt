package eu.darken.octi.kserver.common

import eu.darken.octi.TestRunner
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.junit.jupiter.api.Test
import java.time.Duration

class RateLimiterTest : TestRunner() {

    @Test
    fun `test rate limit allows requests within limit`() = runTest2(
        appConfig = baseConfig.copy(
            rateLimit = RateLimitConfig(limit = 2, resetTime = Duration.ofSeconds(1))
        )
    ) {
        http.get("/v1/status").apply {
            status shouldBe HttpStatusCode.OK
        }

        http.get("/v1/status").apply {
            status shouldBe HttpStatusCode.OK
        }

        http.get("/v1/status").apply {
            status shouldBe HttpStatusCode.TooManyRequests
            bodyAsText() shouldBe "Rate limit exceeded. Try again later."
        }
    }

    @Test
    fun `test rate limit resets after time window`() = runTest2(
        appConfig = baseConfig.copy(
            rateLimit = RateLimitConfig(limit = 2, resetTime = Duration.ofSeconds(1))
        )
    ) {
        repeat(2) {
            http.get("/v1/status").apply {
                status shouldBe HttpStatusCode.OK
            }
        }

        http.get("/v1/status").apply {
            status shouldBe HttpStatusCode.TooManyRequests
        }

        Thread.sleep(1100)

        http.get("/v1/status").apply {
            status shouldBe HttpStatusCode.OK
        }
    }

    @Test
    fun `test different IPs have separate rate limits`() = runTest2(
        appConfig = baseConfig.copy(
            rateLimit = RateLimitConfig(limit = 2, resetTime = Duration.ofSeconds(1))
        )
    ) {
        repeat(2) {
            http.get("/v1/status") {
                header("X-Forwarded-For", "192.168.1.1")
            }.apply {
                status shouldBe HttpStatusCode.OK
            }
        }

        repeat(2) {
            http.get("/v1/status") {
                header("X-Forwarded-For", "192.168.1.2")
            }.apply {
                status shouldBe HttpStatusCode.OK
            }
        }

        http.get("/v1/status") {
            header("X-Forwarded-For", "192.168.1.1")
        }.apply {
            status shouldBe HttpStatusCode.TooManyRequests
        }

        http.get("/v1/status") {
            header("X-Forwarded-For", "192.168.1.2")
        }.apply {
            status shouldBe HttpStatusCode.TooManyRequests
        }
    }
}