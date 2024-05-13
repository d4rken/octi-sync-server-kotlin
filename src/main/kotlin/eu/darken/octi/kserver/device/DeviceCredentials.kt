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
    get() {
        val authHeader = call.request.headers["Authorization"] ?: return null

        if (!authHeader.startsWith("Basic ")) {
            log(WARN) { "Invalid Authorization header: $authHeader" }
            return null
        }

        val base64Credentials = authHeader.removePrefix("Basic ")
        val credentials = Base64.getDecoder().decode(base64Credentials).toString(StandardCharsets.UTF_8)
        val (username, password) = credentials.split(":", limit = 2)

        return DeviceCredentials(
            accountId = UUID.fromString(username),
            devicePassword = password,
        )
    }
