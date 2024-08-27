package eu.darken.octi.kserver.module

import eu.darken.octi.kserver.BaseServerTest
import eu.darken.octi.kserver.addCredentials
import eu.darken.octi.kserver.createDevice
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.junit.jupiter.api.Test
import java.util.*

class ModuleFlowTest : BaseServerTest() {

    private val endPoint = "/v1/modules"
    private val moduleId1 = UUID.randomUUID().toString()

    @Test
    fun `get module requires target device id`() = runTest2 {
        val creds = createDevice()
        get("$endPoint/$moduleId1") {
            addCredentials(creds)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            bodyAsText() shouldBe "Target device id not supplied"
        }
    }

    @Test
    fun `get module - target device needs to exist`() = runTest2 {
        TODO()
    }

    @Test
    fun `get module - target device needs to be on the same account`() = runTest2 {
        TODO()
    }

    @Test
    fun `get module - no data`() = runTest2 {
        val creds = createDevice()
        get {
            url {
                takeFrom("$endPoint/$moduleId1")
                parameters.append("device-id", creds.deviceId.toString())
            }
            addCredentials(creds)
        }.apply {
            bodyAsText() shouldBe "Target device id not supplied"
            TODO()
        }
    }

    @Test
    fun `get module - same device`() = runTest2 {
        val creds = createDevice()
        get {
            url {
                takeFrom("$endPoint/$moduleId1")
                parameters.append("device-id", creds.deviceId.toString())
            }
            addCredentials(creds)
        }.apply {
            bodyAsText() shouldBe "Target device id not supplied"
            TODO("last modified header")
        }
    }

    @Test
    fun `get module - other device`() = runTest2 {
        TODO()
    }

    @Test
    fun `set module - target id is required`() = runTest2 {
        TODO()
    }

    @Test
    fun `set module - target device needs to exist`() = runTest2 {
        TODO()
    }

    @Test
    fun `set module - target device needs to be on the same account`() = runTest2 {
        TODO()
    }

    @Test
    fun `set module`() = runTest2 {
        TODO()
    }

    @Test
    fun `set module - overwrite data`() = runTest2 {
        TODO()
    }

    @Test
    fun `set module - payload limit`() = runTest2 {
        TODO()
    }

    @Test
    fun `delete module - target id is required`() = runTest2 {
        TODO()
    }

    @Test
    fun `delete module - target device needs to exist`() = runTest2 {
        TODO()
    }

    @Test
    fun `delete module - target device needs to be on the same account`() = runTest2 {
        TODO()
    }

    @Test
    fun `delete module`() = runTest2 {
        TODO()
    }
}