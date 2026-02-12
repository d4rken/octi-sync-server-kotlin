package eu.darken.octi.kserver.common

import io.ktor.server.request.*

fun ApplicationRequest.clientIp(): String {
    val connectionIp = local.remoteAddress
    return if (IpHelper.isLoopback(connectionIp)) {
        headers["X-Real-IP"]?.trim() ?: connectionIp
    } else {
        connectionIp
    }
}

object IpHelper {

    private val LOOPBACK_ADDRESSES = setOf("127.0.0.1", "::1", "0:0:0:0:0:0:0:1")

    fun isLoopback(ip: String): Boolean = ip.trimStart('/') in LOOPBACK_ADDRESSES

    private val IP_REGEX = Regex(
        """^(\d{1,3}\.){3}\d{1,3}$|^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$"""
    )

    fun isValid(ip: String): Boolean = IP_REGEX.matches(ip)
}
