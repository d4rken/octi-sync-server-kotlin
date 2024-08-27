package eu.darken.octi.kserver.module

import eu.darken.octi.TestRunner
import eu.darken.octi.createDevice
import eu.darken.octi.readModule
import eu.darken.octi.writeModule
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Duration

class ModuleRepoTest : TestRunner() {

    @Test
    fun `module data can expire`() = runTest2(
        appConfig = baseConfig.copy(
            moduleExpiration = Duration.ofSeconds(2),
            moduleGCInterval = Duration.ofSeconds(1),
        ),
    ) {
        val creds = createDevice()
        writeModule(creds, "abc", data = "test")
        readModule(creds, "abc") shouldBe "test"
        Thread.sleep(config.moduleExpiration.toMillis() + 1000)
        readModule(creds, "abc") shouldBe ""
    }
}