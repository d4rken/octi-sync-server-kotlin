package eu.darken.octi.kserver.account

import eu.darken.octi.kserver.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.statement.*
import io.ktor.http.*
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText

class ShareRepoTest : BaseServerTest() {

    @Test
    fun `shares expire`() = runTest2(
        appConfig = baseConfig.copy(
            account = AccountConfig(
                shareExpirationTime = Duration.ofSeconds(2),
                shareGCInterval = Duration.ofSeconds(2),
            )
        )
    ) {
        val creds1 = createDevice()
        val share1 = createShareCode(creds1)
        Thread.sleep(config.account.shareExpirationTime.toMillis() + 1000)
        createDeviceRaw(shareCode = share1).apply {
            status shouldBe HttpStatusCode.Forbidden
            bodyAsText() shouldBe "Invalid ShareCode"
        }
        val share2 = createShareCode(creds1)
        createDevice(shareCode = share2) shouldNotBe null
    }

    @Test
    fun `shares is saved to disk`() = runTest2 {
        val creds1 = createDevice()
        val shareCode = createShareCode(creds1)
        getSharesPath(creds1).apply {
            exists() shouldBe true
            listDirectoryEntries().first().readText() shouldContain shareCode
        }
    }

    @Test
    fun `shares are restored on reboot`() {
        var creds1: Credentials? = null
        var shareCode: String? = null
        runTest2(keepData = true) {
            creds1 = createDevice()
            shareCode = createShareCode(creds1!!)
        }
        runTest2 {
            val creds2 = createDevice(shareCode = shareCode!!)
            creds1!!.account shouldBe creds2.account
        }
    }

    @Test
    fun `shares consumption deletes the file`() = runTest2 {
        val creds1 = createDevice()
        val share1 = createShareCode(creds1)
        createDevice(shareCode = share1)
        getSharesPath(creds1).listDirectoryEntries().isEmpty() shouldBe true
    }
}