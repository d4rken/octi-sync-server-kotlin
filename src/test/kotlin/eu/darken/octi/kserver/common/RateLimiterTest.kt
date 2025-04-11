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
            rateLimit = RateLimitConfig(limit = 2, resetTime = Duration.ofSeconds(5))
        )
    ) {
        http.get("/v1/status").apply {
            status shouldBe HttpStatusCode.OK
        }
        Thread.sleep(100)

        http.get("/v1/status").apply {
            status shouldBe HttpStatusCode.OK
        }
        Thread.sleep(100)

        http.get("/v1/status").apply {
            status shouldBe HttpStatusCode.TooManyRequests
            bodyAsText() shouldBe "Rate limit exceeded. Try again later."
        }
    }

    @Test
    fun `test rate limit resets after time window`() = runTest2(
        appConfig = baseConfig.copy(
            rateLimit = RateLimitConfig(limit = 2, resetTime = Duration.ofSeconds(5))
        )
    ) {
        repeat(2) {
            http.get("/v1/status").apply {
                status shouldBe HttpStatusCode.OK
            }
            Thread.sleep(100)
        }

        http.get("/v1/status").apply {
            status shouldBe HttpStatusCode.TooManyRequests
        }
        Thread.sleep(100)

        Thread.sleep(5100)

        http.get("/v1/status").apply {
            status shouldBe HttpStatusCode.OK
        }
    }

    @Test
    fun `test different IPs have separate rate limits`() = runTest2(
        appConfig = baseConfig.copy(
            rateLimit = RateLimitConfig(limit = 2, resetTime = Duration.ofSeconds(5))
        )
    ) {
        repeat(2) {
            http.get("/v1/status") {
                header("X-Forwarded-For", "192.168.1.1")
            }.apply {
                status shouldBe HttpStatusCode.OK
            }
            Thread.sleep(100)
        }

        repeat(2) {
            http.get("/v1/status") {
                header("X-Forwarded-For", "192.168.1.2")
            }.apply {
                status shouldBe HttpStatusCode.OK
            }
            Thread.sleep(100)
        }

        http.get("/v1/status") {
            header("X-Forwarded-For", "192.168.1.1")
        }.apply {
            status shouldBe HttpStatusCode.TooManyRequests
        }
        Thread.sleep(100)

        http.get("/v1/status") {
            header("X-Forwarded-For", "192.168.1.2")
        }.apply {
            status shouldBe HttpStatusCode.TooManyRequests
        }
    }

    @Test
    fun `test stale rate limit entries are cleaned up`() = runTest2(
        appConfig = baseConfig.copy(
            rateLimit = RateLimitConfig(limit = 2, resetTime = Duration.ofSeconds(2))
        )
    ) {
        // Make requests from two different IPs
        http.get("/v1/status") {
            header("X-Forwarded-For", "192.168.1.1")
        }.apply {
            status shouldBe HttpStatusCode.OK
        }
        Thread.sleep(100)

        http.get("/v1/status") {
            header("X-Forwarded-For", "192.168.1.2")
        }.apply {
            status shouldBe HttpStatusCode.OK
        }
        Thread.sleep(100)

        // Wait for entries to become stale (2.5 seconds > resetTime of 2 seconds)
        Thread.sleep(2500)

        // Make new requests from both IPs
        http.get("/v1/status") {
            header("X-Forwarded-For", "192.168.1.1")
        }.apply {
            status shouldBe HttpStatusCode.OK
        }
        Thread.sleep(100)

        http.get("/v1/status") {
            header("X-Forwarded-For", "192.168.1.2")
        }.apply {
            status shouldBe HttpStatusCode.OK
        }
        Thread.sleep(100)

        // Both IPs should be able to make requests again since their entries were cleaned up
        http.get("/v1/status") {
            header("X-Forwarded-For", "192.168.1.1")
        }.apply {
            status shouldBe HttpStatusCode.OK
        }
        Thread.sleep(100)

        http.get("/v1/status") {
            header("X-Forwarded-For", "192.168.1.2")
        }.apply {
            status shouldBe HttpStatusCode.OK
        }
    }
}