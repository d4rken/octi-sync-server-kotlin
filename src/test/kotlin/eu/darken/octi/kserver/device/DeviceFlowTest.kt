package eu.darken.octi.kserver.device

import eu.darken.octi.*
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import org.junit.jupiter.api.Test
import java.util.*

class DeviceFlowTest : TestRunner() {

    private val endPoint = "/v1/devices"

    @Test
    fun `get devices`() = runTest2 {
        val creds1 = createDevice()
        val creds2 = createDevice(creds1)
        getDevices(creds1) shouldBe TestDevices(
            setOf(
                TestDevices.Device(creds1.deviceId),
                TestDevices.Device(creds2.deviceId),
            )
        )
    }

    @Test
    fun `get devices - requires valid auth`() = runTest2 {
        val creds1 = createDevice()
        http.get(endPoint) {
            addDeviceId(creds1.deviceId)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `delete device`() = runTest2 {
        val creds1 = createDevice()
        val creds2 = createDevice(creds1)
        getDevices(creds1).devices.size shouldBe 2

        deleteDevice(creds1, creds2.deviceId)

        getDevices(creds1).devices.size shouldBe 1
    }

    @Test
    fun `delete devices - requires valid auth`() = runTest2 {
        val creds1 = createDevice()
        http.delete("$endPoint/${UUID.randomUUID()}") {
            addDeviceId(creds1.deviceId)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `deleting ourselves`() = runTest2 {
        val creds1 = createDevice()

        deleteDevice(creds1)

        http.get(endPoint) {
            addCredentials(creds1)
        }.apply {
            status shouldBe HttpStatusCode.NotFound
        }
    }
}