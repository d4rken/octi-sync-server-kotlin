package eu.darken.octi.kserver

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.junit.jupiter.api.Test

class ServerTest : BaseServerTest() {

    @Test
    fun `base route test`() = runTest2 {
        client.get("/v1").apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldStartWith "ello "
        }
    }
}