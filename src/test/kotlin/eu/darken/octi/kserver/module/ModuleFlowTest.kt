package eu.darken.octi.kserver.module

import eu.darken.octi.kserver.*
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.junit.jupiter.api.Test
import java.util.*

class ModuleFlowTest : BaseServerTest() {

    private val endpointModules = "/v1/modules"

    @Test
    fun `module id format needs to match`() = runTest2 {
        val creds = createDevice()
        readModule(creds, "123").apply {
            status shouldBe HttpStatusCode.BadRequest
            bodyAsText() shouldBe "Invalid moduleId"
        }
        readModule(creds, "abc...").apply {
            status shouldBe HttpStatusCode.BadRequest
            bodyAsText() shouldBe "Invalid moduleId"
        }
        readModule(creds, "eu.darken.octi.module.core.meta").apply {
            status shouldBe HttpStatusCode.NoContent
        }
    }

    @Test
    fun `module ids have size limits`() = runTest2 {
        val creds = createDevice()
        readModule(creds, "a".repeat(1025)).apply {
            status shouldBe HttpStatusCode.BadRequest
            bodyAsText() shouldBe "Invalid moduleId"
        }
        readModule(creds, "a".repeat(1024)).apply {
            status shouldBe HttpStatusCode.NoContent
        }
    }

    @Test
    fun `get module requires target device id`() = runTest2 {
        val creds = createDevice()
        http.get {
            url {
                takeFrom("$endpointModules/abc")
            }
            addCredentials(creds)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            bodyAsText() shouldBe "Target device id not supplied"
        }
    }

    @Test
    fun `get module - target device needs to exist`() = runTest2 {
        val creds = createDevice()
        readModule(creds, "abc", deviceId = UUID.randomUUID()).apply {
            status shouldBe HttpStatusCode.NotFound
            bodyAsText() shouldBe "Target device not found"
        }
    }

    @Test
    fun `get module - target device needs to be on the same account`() = runTest2 {
        val creds1 = createDevice()
        val creds2 = createDevice()
        readModule(creds1, "abc", creds2.deviceId).apply {
            status shouldBe HttpStatusCode.Unauthorized
            bodyAsText() shouldBe "Devices don't share the same account"
        }
    }

    @Test
    fun `get module - no data`() = runTest2 {
        val creds = createDevice()
        readModule(creds, "abc").apply {
            status shouldBe HttpStatusCode.NoContent
            bodyAsText() shouldBe ""
        }
    }

    @Test
    fun `write and read module`() = runTest2 {
        val creds = createDevice()
        val testData = UUID.randomUUID().toString()
        writeModule(creds, "abc", body = testData).apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe ""
        }
        readModule(creds, "abc").apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe testData
        }
    }

    @Test
    fun `get data from other devices modules`() = runTest2 {
        val creds1 = createDevice()
        writeModule(creds1, "abc", creds1.deviceId, "test")
        val creds2 = createDevice(creds1)
        readModule(creds2, "abc", creds1.deviceId).bodyAsText() shouldBe "test"
    }

    @Test
    fun `set module - target id is required`() = runTest2 {
        val creds = createDevice()
        writeModule(creds, "abc", deviceId = null, "test").apply {
            status shouldBe HttpStatusCode.BadRequest
            bodyAsText() shouldBe "Target device id not supplied"
        }
    }

    @Test
    fun `set module - target device needs to exist`() = runTest2 {
        val creds = createDevice()
        writeModule(creds, "abc", UUID.randomUUID(), "test").apply {
            status shouldBe HttpStatusCode.NotFound
            bodyAsText() shouldBe "Target device not found"
        }
    }

    @Test
    fun `set module - target device needs to be on the same account`() = runTest2 {
        val creds1 = createDevice()
        val creds2 = createDevice()
        writeModule(creds2, "abc", creds1.deviceId, "test").apply {
            status shouldBe HttpStatusCode.Unauthorized
            bodyAsText() shouldBe "Devices don't share the same account"
        }
        writeModule(creds2, "abc", creds1.deviceId, "test")
    }


    @Test
    fun `set module - overwrite data`() = runTest2 {
        val creds = createDevice()
        writeModule(creds, "abc", body = "test1")
        readModule(creds, "abc").bodyAsText() shouldBe "test1"
        writeModule(creds, "abc", body = "test2")
        readModule(creds, "abc").bodyAsText() shouldBe "test2"
    }

    @Test
    fun `set module - payload limit`() = runTest2 {
        val creds = createDevice()
        writeModule(creds, "abc", body = "a".repeat((128 * 1024) + 1)).apply {
            status shouldBe HttpStatusCode.PayloadTooLarge
        }
        writeModule(creds, "abc", body = "a".repeat(128 * 1024)).apply {
            status shouldBe HttpStatusCode.OK
        }
    }

    @Test
    fun `delete module - target id is required`() = runTest2 {
        val creds = createDevice()
        deleteModule(creds, "abc", deviceId = null).apply {
            status shouldBe HttpStatusCode.BadRequest
            bodyAsText() shouldBe "Target device id not supplied"
        }
    }

    @Test
    fun `delete module - target device needs to exist`() = runTest2 {
        val creds = createDevice()
        deleteModule(creds, "abc", deviceId = UUID.randomUUID()).apply {
            status shouldBe HttpStatusCode.NotFound
            bodyAsText() shouldBe "Target device not found"
        }
    }

    @Test
    fun `delete module - target device needs to be on the same account`() = runTest2 {
        val creds1 = createDevice()
        val creds2 = createDevice()
        deleteModule(creds2, "abc", creds1.deviceId).apply {
            status shouldBe HttpStatusCode.Unauthorized
            bodyAsText() shouldBe "Devices don't share the same account"
        }
    }

    @Test
    fun `delete module`() = runTest2 {
        val creds = createDevice()
        writeModule(creds, "abc", body = "test")
        readModule(creds, "abc").bodyAsText() shouldBe "test"
        deleteModule(creds, "abc")
        readModule(creds, "abc").bodyAsText() shouldBe ""
    }

    @Test
    fun `delete from other devices`() = runTest2 {
        val creds1 = createDevice()
        val creds2 = createDevice()
        writeModule(creds1, "abc", body = "test")
        readModule(creds1, "abc").bodyAsText() shouldBe "test"
        deleteModule(creds2, "abc", creds1.deviceId).apply {
            status shouldBe HttpStatusCode.Unauthorized
            bodyAsText() shouldBe "Devices don't share the same account"
        }
        readModule(creds1, "abc").bodyAsText() shouldBe "test"
    }
}