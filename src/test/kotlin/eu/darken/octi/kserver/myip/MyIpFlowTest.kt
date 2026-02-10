package eu.darken.octi.kserver.myip

import eu.darken.octi.TestRunner
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test

class MyIpFlowTest : TestRunner() {

    @Test
    fun `get my ip`() = runTest2 {
        http.get("/v1/myip").apply {
            status shouldBe HttpStatusCode.OK
            val json = Json.parseToJsonElement(bodyAsText()).jsonObject
            json.containsKey("ip") shouldBe true
            json["ip"]!!.jsonPrimitive.content.shouldNotBeBlank()
        }
    }
}
