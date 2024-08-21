package eu.darken.octi.kserver.account

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText

class AccountShareFlowTest : BaseServerTest() {

    private val endpointAcc = "/v1/account"
    private val endpointShare = "$endpointAcc/share"

    @Test
    fun `linking via sharecode`() = runTest2 {
        val creds1 = createDevice()

        val shareCode = createShareCode(creds1)

        creds1.getAccountPath().resolve("shares").apply {
            exists() shouldBe true
            listDirectoryEntries().first().readText() shouldContain shareCode
        }

        val creds2 = createDevice(shareCode = shareCode)

        creds1.account shouldBe creds2.account
    }

    @Test
    fun `no cross use`() = runTest2 {
        val creds1 = createDevice()

        val shareCode = createShareCode(creds1)

        val creds2 = createDevice()
        val creds3 = createDevice(shareCode = shareCode)

        creds1.account shouldNotBe creds2.account
        creds2.account shouldNotBe creds3.account
    }

    @Test
    fun `creating share requires matching auth`() = runTest2 {
        val creds1 = createDevice()

        post(endpointShare) {
            addDeviceId(UUID.randomUUID())
            addAuth(creds1.auth)
        }.apply {
            status shouldBe HttpStatusCode.NotFound
            bodyAsText() shouldContain "Unknown device"
        }
    }

    @Test
    fun `creating share requires valid auth`() = runTest2 {
        val creds1 = createDevice()

        post(endpointShare) {
            addDeviceId(creds1.deviceId)
            addAuth(creds1.auth.copy(password = "abc"))
        }.apply {
            status shouldBe HttpStatusCode.Unauthorized
            bodyAsText() shouldContain "Device credentials not found or insufficient"
        }
    }

    @Test
    fun `no double register`() = runTest2 {
        val creds1 = createDevice()
        val shareCode = createShareCode(creds1)

        val creds2 = createDevice(shareCode = shareCode)

        creds1.account shouldBe creds2.account

        post {
            url {
                takeFrom(endpointAcc)
                parameters.append("share", shareCode)
            }
            addDeviceId(creds2.deviceId)
        }.apply {
            status shouldBe HttpStatusCode.BadRequest
            bodyAsText() shouldContain "Device is already registered"
        }
    }

    @Test
    fun `no double use`() = runTest2 {
        val creds1 = createDevice()
        val shareCode1 = createShareCode(creds1)

        val creds2 = post {
            url {
                takeFrom(endpointAcc)
                parameters.append("share", shareCode1)
            }
            addDeviceId(UUID.randomUUID())
        }.asAuth()

        creds1.account shouldBe creds2.account

        post {
            url {
                takeFrom(endpointAcc)
                parameters.append("share", shareCode1)
            }
            addDeviceId(UUID.randomUUID())
        }.apply {
            status shouldBe HttpStatusCode.Forbidden
            bodyAsText() shouldContain "Invalid ShareCode"
        }
    }
}