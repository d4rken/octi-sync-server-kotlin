package eu.darken.octi.kserver.account

import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.*

suspend fun HttpResponse.asMap() = Json.decodeFromString<Map<String, String>>(bodyAsText())

@Serializable
data class Credentials(
    val account: String,
    val password: String
)

suspend fun HttpResponse.asCredentials() = Json.decodeFromString<Credentials>(bodyAsText())

fun Credentials.toBearerToken(): String {
    val credentials = "$account:$password"
    val encodedCredentials = Base64.getEncoder().encodeToString(credentials.toByteArray())
    return "Basic $encodedCredentials"
}

fun HttpRequestBuilder.addDeviceId(id: String) {
    headers {
        append("X-Device-ID", id)
    }
}

fun HttpRequestBuilder.addAuth(credentials: Credentials) {
    headers {
        append("Authorization", credentials.toBearerToken())
    }
}