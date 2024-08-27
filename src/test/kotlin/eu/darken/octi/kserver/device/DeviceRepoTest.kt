package eu.darken.octi.kserver.device

import eu.darken.octi.TestRunner
import eu.darken.octi.createDevice
import eu.darken.octi.getDevices
import eu.darken.octi.getDevicesRaw
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import io.ktor.client.statement.*
import io.ktor.http.*
import org.junit.jupiter.api.Test
import java.time.Duration

class DeviceRepoTest : TestRunner() {

    @Test
    fun `old devices are deleted`() = runTest2(
        appConfig = baseConfig.copy(
            deviceExpiration = Duration.ofSeconds(2),
            deviceGCInterval = Duration.ofSeconds(1),
        ),
    ) {
        val creds1 = createDevice()
        getDevices(creds1) shouldNotBe null
        Thread.sleep(config.deviceExpiration.toMillis() - 100)
        getDevices(creds1) shouldNotBe null
        Thread.sleep(config.deviceExpiration.toMillis() + 1000)
        getDevicesRaw(creds1).apply {
            status shouldBe HttpStatusCode.NotFound
            bodyAsText() shouldStartWith "Unknown device"
        }
    }
}