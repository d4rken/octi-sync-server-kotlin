package eu.darken.octi.kserver.device

import eu.darken.octi.*
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.io.path.exists

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
    fun `deleting ourselves`() = runTest2 {
        val creds1 = createDevice()
        deleteDevice(creds1)
        http.get(endPoint) {
            addCredentials(creds1)
        }.apply {
            status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `delete other device`() = runTest2 {
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
    fun `deleting device wipes module data`() = runTest2 {
        val creds1 = createDevice()

        getModulesPath(creds1).exists() shouldBe false
        writeModule(creds1, "abc", data = "test")
        getModulesPath(creds1).exists() shouldBe true

        deleteDevice(creds1)
        getModulesPath(creds1).exists() shouldBe false
    }

    @Test
    fun `resetting devices`() = runTest2 {
        val creds1 = createDevice()
        val creds2 = createDevice(creds1)
        writeModule(creds1, "abc", data = "test")
        writeModule(creds2, "abc", data = "test")
        http.post("$endPoint/reset") {
            addCredentials(creds1)
            contentType(ContentType.Application.Json)
            setBody(setOf(creds1.deviceId.toString(), creds2.deviceId.toString()))
        }
        readModule(creds1, "abc") shouldBe ""
        readModule(creds2, "abc") shouldBe ""
    }

    @Test
    fun `resetting devices without specific targets`() = runTest2 {
        val creds1 = createDevice()
        val creds2 = createDevice(creds1)
        writeModule(creds1, "abc", data = "test")
        writeModule(creds2, "abc", data = "test")
        http.post("$endPoint/reset") {
            addCredentials(creds1)
            contentType(ContentType.Application.Json)
            setBody("{targets: []}")
        }
        readModule(creds1, "abc") shouldBe ""
        readModule(creds2, "abc") shouldBe ""
    }
}