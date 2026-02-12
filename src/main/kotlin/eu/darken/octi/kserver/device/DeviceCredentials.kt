package eu.darken.octi.kserver.device

import eu.darken.octi.kserver.account.AccountId
import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.WARN
import eu.darken.octi.kserver.common.debug.logging.log
import io.ktor.server.routing.*
import java.nio.charset.StandardCharsets
import java.util.*

data class DeviceCredentials(
    val accountId: AccountId,
    val devicePassword: String,
)


val RoutingContext.deviceCredentials: DeviceCredentials?
    get() = try {
        val authHeader = call.request.headers["Authorization"] ?: return null

        if (!authHeader.startsWith("Basic ")) {
            log(WARN) { "Invalid Authorization scheme (header length: ${authHeader.length})" }
            return null
        }

        val decoded = Base64.getDecoder().decode(authHeader.removePrefix("Basic "))
            .toString(StandardCharsets.UTF_8)
        val parts = decoded.split(":", limit = 2)
        if (parts.size != 2) return null

        DeviceCredentials(
            accountId = UUID.fromString(parts[0]),
            devicePassword = parts[1],
        )
    } catch (e: Exception) {
        log(WARN) { "Failed to parse credentials" }
        null
    }
