package eu.darken.octi.kserver.account

import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import org.junit.jupiter.api.Test

class StatusFlowTest : BaseServerTest() {

    @Test
    fun `get status`() = runTest2 {
        get("/v1/status") {

        }.apply {
            status shouldBe HttpStatusCode.OK
        }
    }
}