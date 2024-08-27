package eu.darken.octi.kserver.account

import eu.darken.octi.*
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldStartWith
import io.ktor.client.statement.*
import io.ktor.http.*
import org.junit.jupiter.api.Test
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.readText

class AccountRepoTest : TestRunner() {

    @Test
    fun `creating a new account`() = runTest2 {
        val creds = createDevice()
        getAccountPath(creds).apply {
            exists() shouldBe true
            resolve("account.json").apply {
                exists() shouldBe true
                fileSize() shouldBeGreaterThan 64L
                readText() shouldMatch """
                    \{
                        \s*"id":\s*"[0-9a-fA-F-]{36}",
                        \s*"createdAt":\s*"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{6}Z"
                    \s*\}
                """.trimIndent()
            }
        }
    }

    @Test
    fun `deleting an account`() = runTest2 {
        val creds1 = createDevice()
        val creds2 = createDevice()
        writeModule(creds2, "abc", data = "test")
        getAccountPath(creds1).exists() shouldBe true
        deleteAccount(creds1)
        getAccountPath(creds1).exists() shouldBe false
        readModule(creds2, "abc") shouldBe "test"
        getAccountPath(creds2).exists() shouldBe true
    }

    @Test
    fun `accounts are restored`() {
        var creds1: Credentials? = null
        var creds2: Credentials? = null
        runTest2(keepData = true) {
            creds1 = createDevice()
            creds2 = createDevice(creds1!!)
            getDevices(creds1!!) shouldBe TestDevices(
                setOf(
                    TestDevices.Device(creds1!!.deviceId),
                    TestDevices.Device(creds2!!.deviceId),
                )
            )
        }
        runTest2 {
            getDevices(creds1!!) shouldBe TestDevices(
                setOf(
                    TestDevices.Device(creds1!!.deviceId),
                    TestDevices.Device(creds2!!.deviceId),
                )
            )
        }
    }

    @Test
    fun `empty accounts are deleted`() = runTest2 {
        val creds1 = createDevice()
        getDevices(creds1) shouldNotBe null
        deleteDevice(creds1)
        getDevicesRaw(creds1).apply {
            status shouldBe HttpStatusCode.NotFound
            bodyAsText() shouldStartWith "Unknown device"
        }
    }

}