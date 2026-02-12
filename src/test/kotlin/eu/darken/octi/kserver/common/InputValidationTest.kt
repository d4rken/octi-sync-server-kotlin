package eu.darken.octi.kserver.common

import eu.darken.octi.*
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.junit.jupiter.api.Test
import java.util.*

class InputValidationTest : TestRunner() {

    // --- Malformed Authorization header ---

    @Test
    fun `malformed base64 in auth header returns 400`() = runTest2 {
        val creds = createDevice()
        http.get("/v1/devices") {
            addDeviceId(creds.deviceId)
            headers { append("Authorization", "Basic !!!not-base64!!!") }
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            bodyAsText() shouldBe "Device credentials are missing"
        }
    }

    @Test
    fun `auth header missing colon separator returns 400`() = runTest2 {
        val creds = createDevice()
        val noColon = Base64.getEncoder().encodeToString("nocolonhere".toByteArray())
        http.get("/v1/devices") {
            addDeviceId(creds.deviceId)
            headers { append("Authorization", "Basic $noColon") }
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            bodyAsText() shouldBe "Device credentials are missing"
        }
    }

    @Test
    fun `auth header with non-UUID username returns 400`() = runTest2 {
        val creds = createDevice()
        val badUsername = Base64.getEncoder().encodeToString("not-a-uuid:somepassword".toByteArray())
        http.get("/v1/devices") {
            addDeviceId(creds.deviceId)
            headers { append("Authorization", "Basic $badUsername") }
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            bodyAsText() shouldBe "Device credentials are missing"
        }
    }

    @Test
    fun `auth header with wrong scheme returns 400`() = runTest2 {
        val creds = createDevice()
        http.get("/v1/devices") {
            addDeviceId(creds.deviceId)
            headers { append("Authorization", "Bearer some-token") }
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            bodyAsText() shouldBe "Device credentials are missing"
        }
    }

    // --- Invalid UUID in device path parameter ---

    @Test
    fun `invalid UUID in device delete path returns 400`() = runTest2 {
        val creds = createDevice()
        http.delete("/v1/devices/not-a-uuid") {
            addCredentials(creds)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            bodyAsText() shouldBe "Invalid device ID format"
        }
    }

    // --- Invalid UUID in module device-id query parameter ---

    @Test
    fun `invalid UUID in module device-id query param returns 400`() = runTest2 {
        val creds = createDevice()
        http.get {
            url {
                takeFrom("/v1/module/eu.darken.test")
                parameters.append("device-id", "not-a-uuid")
            }
            addCredentials(creds)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            bodyAsText() shouldBe "Invalid device ID format"
        }
    }

    // --- Unknown device IDs in reset ---

    @Test
    fun `reset with unknown device ID returns 404`() = runTest2 {
        val creds = createDevice()
        val unknownId = UUID.randomUUID()
        http.post("/v1/devices/reset") {
            addCredentials(creds)
            contentType(ContentType.Application.Json)
            setBody("{\"targets\": [\"$unknownId\"]}")
        }.apply {
            status shouldBe HttpStatusCode.NotFound
            bodyAsText() shouldBe "Device not found: $unknownId"
        }
    }
}
