package eu.darken.octi.kserver.common

import io.ktor.server.request.*

fun ApplicationRequest.clientIp(): String {
    val connectionIp = local.remoteAddress
    if (!IpHelper.isLoopback(connectionIp)) return connectionIp

    val forwardedIp = headers["X-Real-IP"]?.trim()
        ?: headers["X-Forwarded-For"]?.split(",")?.lastOrNull()?.trim()

    return forwardedIp?.takeIf { IpHelper.isValid(it) } ?: connectionIp
}

object IpHelper {

    private val LOOPBACK_ADDRESSES = setOf("127.0.0.1", "::1", "0:0:0:0:0:0:0:1")

    fun isLoopback(ip: String): Boolean = ip.trimStart('/') in LOOPBACK_ADDRESSES

    private val IP_REGEX = Regex(
        """^(\d{1,3}\.){3}\d{1,3}$|^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$"""
    )

    fun isValid(ip: String): Boolean = IP_REGEX.matches(ip)
}
