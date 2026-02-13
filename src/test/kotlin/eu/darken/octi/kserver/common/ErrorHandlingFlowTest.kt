package eu.darken.octi.kserver.common

import eu.darken.octi.*
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import org.junit.jupiter.api.Test

class ErrorHandlingFlowTest : TestRunner() {

    @Test
    fun `oversized payload returns 413`() = runTest2 {
        val creds = createDevice()
        http.post {
            url { takeFrom("/v1/account") }
            addDeviceId(creds.deviceId)
            contentType(ContentType.Application.OctetStream)
            setBody("a".repeat((128 * 1024) + 1))
        }.apply {
            status shouldBe HttpStatusCode.PayloadTooLarge
        }
    }

    @Test
    fun `unknown route returns 404`() = runTest2 {
        http.get("/v1/nonexistent").apply {
            status shouldBe HttpStatusCode.NotFound
        }
    }
}
