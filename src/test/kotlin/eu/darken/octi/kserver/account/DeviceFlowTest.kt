package eu.darken.octi.kserver.account

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.junit.jupiter.api.Test
import java.util.*

class DeviceFlowTest : BaseServerTest() {

    private val endPoint = "/v1/devices"

    data class DevicesResponse(
        val devices: List<Device>
    )

    data class Device(
        val id: String,
        val version: String
    )

    @Test
    fun `get devices`() = runTest2 {
        val creds1 = createDevice()
        val creds2 = createDevice(creds1)
        get(endPoint) {
            addCredentials(creds1)
        }.apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldContain Regex("""\{\s*"devices":\s*\[\s*\{\s*"id":\s*"[a-f0-9\-]{36}",\s*"version":\s*".+?"\s*\}(\s*,\s*\{\s*"id":\s*"[a-f0-9\-]{36}",\s*"version":\s*".+?"\s*\})*\s*]\s*\}""")
        }
    }

    @Test
    fun `get devices - requires valid auth`() = runTest2 {
        val creds1 = createDevice()
        get(endPoint) {
            addDeviceId(creds1.deviceId)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `delete device`() = runTest2 {
        val creds1 = createDevice()
        val creds2 = createDevice(creds1)
        delete("$endPoint/${creds2.deviceId}") {
            addCredentials(creds1)
        }.apply {
            status shouldBe HttpStatusCode.OK
        }

        get(endPoint) {
            addCredentials(creds1)
        }.apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe """
                {
                    "devices": [
                        {
                            "id": "${creds1.deviceId}",
                            "version": "Ktor client"
                        }
                    ]
                }
            """.trimIndent()
        }
    }

    @Test
    fun `delete devices - requires valid auth`() = runTest2 {
        val creds1 = createDevice()
        delete("$endPoint/${UUID.randomUUID()}") {
            addDeviceId(creds1.deviceId)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `deleting ourselves`() = runTest2 {
        val creds1 = createDevice()

        delete("$endPoint/${creds1.deviceId}") {
            addCredentials(creds1)
        }.apply {
            status shouldBe HttpStatusCode.OK
        }

        get(endPoint) {
            addCredentials(creds1)
        }.apply {
            status shouldBe HttpStatusCode.NotFound
        }
    }
}