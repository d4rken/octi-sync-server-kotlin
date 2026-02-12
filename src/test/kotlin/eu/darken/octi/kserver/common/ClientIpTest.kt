package eu.darken.octi.kserver.common

import io.kotest.matchers.shouldBe
import io.ktor.http.*
import io.ktor.server.request.*
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class ClientIpTest {

    private fun createRequest(
        remoteAddress: String,
        realIp: String? = null,
    ): ApplicationRequest {
        val local = mockk<RequestConnectionPoint> {
            every { this@mockk.remoteAddress } returns remoteAddress
        }
        val headers = HeadersBuilder().apply {
            if (realIp != null) append("X-Real-IP", realIp)
        }.build()
        return mockk {
            every { this@mockk.local } returns local
            every { this@mockk.headers } returns headers
        }
    }

    @Test
    fun `non-loopback returns connection IP`() {
        val request = createRequest(remoteAddress = "203.0.113.5")
        request.clientIp() shouldBe "203.0.113.5"
    }

    @Test
    fun `non-loopback ignores X-Real-IP`() {
        val request = createRequest(remoteAddress = "203.0.113.5", realIp = "10.0.0.1")
        request.clientIp() shouldBe "203.0.113.5"
    }

    @Test
    fun `loopback IPv4 uses X-Real-IP`() {
        val request = createRequest(remoteAddress = "127.0.0.1", realIp = "198.51.100.7")
        request.clientIp() shouldBe "198.51.100.7"
    }

    @Test
    fun `loopback IPv6 uses X-Real-IP`() {
        val request = createRequest(remoteAddress = "0:0:0:0:0:0:0:1", realIp = "198.51.100.7")
        request.clientIp() shouldBe "198.51.100.7"
    }

    @Test
    fun `loopback without X-Real-IP falls back to connection IP`() {
        val request = createRequest(remoteAddress = "127.0.0.1")
        request.clientIp() shouldBe "127.0.0.1"
    }
}
