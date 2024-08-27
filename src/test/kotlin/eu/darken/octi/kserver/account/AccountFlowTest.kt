package eu.darken.octi.kserver.account

import eu.darken.octi.*
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.junit.jupiter.api.Test
import java.util.*

class AccountFlowTest : TestRunner() {

    private val endpoint = "/v1/account"

    @Test
    fun `creating a new account`() = runTest2 {
        createDeviceRaw().apply {
            status shouldBe HttpStatusCode.OK
            asAuth().apply {
                UUID.fromString(account)
                password.length shouldBeGreaterThan 64
            }
        }
    }

    @Test
    fun `create account requires device ID header`() = runTest2 {
        http.post(endpoint).apply {
            status shouldBe HttpStatusCode.BadRequest
            bodyAsText() shouldBe "X-Device-ID header is missing"
        }

        http.post(endpoint) {
            headers {
                append("X-Device-ID", "something")
            }
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            bodyAsText() shouldBe "X-Device-ID header is missing"
        }
    }

    @Test
    fun `no double creation`() = runTest2 {
        val deviceId = UUID.randomUUID()
        http.post(endpoint) {
            addDeviceId(deviceId)
        }
        http.post(endpoint) {
            addDeviceId(deviceId)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            bodyAsText() shouldBe "Device is already registered"
        }
    }

    @Test
    fun `deleting an account`() = runTest2 {
        val creds1 = createDevice()

        http.delete(endpoint) {
            addCredentials(creds1)
        }.apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe ""
        }

        val creds2 = createDevice(creds1.deviceId)

        creds1 shouldNotBe creds2
    }
}