package eu.darken.octi.kserver.account

import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.junit.jupiter.api.Test
import kotlin.io.path.exists
import kotlin.io.path.fileSize

class AccountFlowTest : BaseServerTest() {

    private val endpoint = "/v1/account"
    private val deviceId = "34ac3ec4-7d08-4e7c-99f3-6f07da677307"
    private val accountsPath = dataPath.resolve("accounts")

    @Test
    fun `creating a new account`() = runTest2 {
        val creds = post(endpoint) {
            addDeviceId(deviceId)
            addUserAgent()
        }.apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldMatch """\{\s*"account":\s*"[0-9a-fA-F-]{36}",\s*"password":\s*"[0-9a-fA-F]{128}"\s*\}""".toRegex()
        }.asCredentials()
        accountsPath.resolve(creds.account).apply {
            exists() shouldBe true
            resolve("account.json").apply {
                exists() shouldBe true
                fileSize() shouldBeGreaterThan 64L
            }
        }
    }

    @Test
    fun `create account requires device ID header`() = runTest2 {
        post(endpoint).apply {
            status shouldBe HttpStatusCode.BadRequest
            bodyAsText() shouldBe "X-Device-ID header is missing"
        }

        post(endpoint) {
            addDeviceId("something")
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            bodyAsText() shouldBe "X-Device-ID header is missing"
        }
    }

    @Test
    fun `no double creation`() = runTest2 {
        post(endpoint) {
            addDeviceId(deviceId)
        }
        post(endpoint) {
            addDeviceId(deviceId)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            bodyAsText() shouldBe "Device is already registered"
        }
    }

    @Test
    fun `deleting an account`() = runTest2 {
        val creds1 = post(endpoint) {
            addDeviceId(deviceId)
        }.asCredentials()
        accountsPath.resolve(creds1.account).apply {
            exists() shouldBe true
            resolve("account.json").exists() shouldBe true
        }
        delete(endpoint) {
            addDeviceId(deviceId)
            addAuth(creds1)
        }.apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe ""
        }

        accountsPath.resolve(creds1.account).apply {
            exists() shouldBe false
        }

        val creds2 = post(endpoint) {
            addDeviceId(deviceId)
        }.asCredentials()

        creds1 shouldNotBe creds2
    }
}