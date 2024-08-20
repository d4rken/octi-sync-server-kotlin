package eu.darken.octi.kserver.account

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.junit.jupiter.api.Test
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText

class AccountShareFlowTest : BaseServerTest() {

    private val endpointAcc = "/v1/account"
    private val endpointShare = "$endpointAcc/share"
    private val deviceId1 = "34ac3ec4-7d08-4e7c-99f3-6f07da677307"
    private val deviceId2 = "d25f7800-00b2-410c-94af-d02c5ed8a529"
    private val deviceId3 = "d402f8aa-b609-4e42-8732-b02823151626"
    private val accountsPath = dataPath.resolve("accounts")

    @Test
    fun `linking via sharecode`() = runTest2 {
        val creds1 = post(endpointAcc) { addDeviceId(deviceId1) }.asCredentials()
        val accPath = accountsPath.resolve(creds1.account)

        val shareCode = post(endpointShare) {
            addDeviceId(deviceId1)
            addAuth(creds1)
        }.asMap()["code"]!!

        accPath.resolve("shares").apply {
            exists() shouldBe true
            listDirectoryEntries().first().readText() shouldContain shareCode
        }

        val creds2 = post {
            url {
                takeFrom(endpointAcc)
                parameters.append("share", shareCode)
            }
            addDeviceId(deviceId2)
        }.asCredentials()

        creds1.account shouldBe creds2.account
    }

    @Test
    fun `no cross use`() = runTest2 {
        val creds1 = post(endpointAcc) { addDeviceId(deviceId1) }.asCredentials()

        val shareCode1 = post(endpointShare) {
            addDeviceId(deviceId1)
            addAuth(creds1)
        }.asMap()["code"]!!

        val creds2 = post(endpointAcc) { addDeviceId(deviceId2) }.asCredentials()
        val creds3 = post {
            url {
                takeFrom(endpointAcc)
                parameters.append("share", shareCode1)
            }
            addDeviceId(deviceId3)
        }.asCredentials()

        creds1.account shouldNotBe creds2.account
        creds2.account shouldNotBe creds3.account
    }

    @Test
    fun `need verified account to create a share`() = runTest2 {
        val creds1 = post(endpointAcc) { addDeviceId(deviceId1) }.asCredentials()

        post(endpointShare) {
            addDeviceId(deviceId2)
            addAuth(creds1)
        }.apply {
            status shouldBe HttpStatusCode.NotFound
            bodyAsText() shouldContain "Unknown device"
        }
    }

    @Test
    fun `no double register`() = runTest2 {
        val creds1 = post(endpointAcc) { addDeviceId(deviceId1) }.asCredentials()
        val shareCode1 = post(endpointShare) {
            addDeviceId(deviceId1)
            addAuth(creds1)
        }.asMap()["code"]!!

        val creds2 = post {
            url {
                takeFrom(endpointAcc)
                parameters.append("share", shareCode1)
            }
            addDeviceId(deviceId2)
        }.asCredentials()

        creds1.account shouldBe creds2.account

        post {
            url {
                takeFrom(endpointAcc)
                parameters.append("share", shareCode1)
            }
            addDeviceId(deviceId2)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            bodyAsText() shouldContain "Device is already registered"
        }
    }

    @Test
    fun `no double use`() = runTest2 {
        val creds1 = post(endpointAcc) { addDeviceId(deviceId1) }.asCredentials()
        val shareCode1 = post(endpointShare) {
            addDeviceId(deviceId1)
            addAuth(creds1)
        }.asMap()["code"]!!

        val creds2 = post {
            url {
                takeFrom(endpointAcc)
                parameters.append("share", shareCode1)
            }
            addDeviceId(deviceId2)
        }.asCredentials()

        creds1.account shouldBe creds2.account

        post {
            url {
                takeFrom(endpointAcc)
                parameters.append("share", shareCode1)
            }
            addDeviceId(deviceId3)
        }.apply {
            status shouldBe HttpStatusCode.Forbidden
            bodyAsText() shouldContain "Invalid ShareCode"
        }
    }
}