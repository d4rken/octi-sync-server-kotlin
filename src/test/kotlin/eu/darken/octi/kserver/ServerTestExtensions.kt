package eu.darken.octi.kserver

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.*

suspend fun HttpResponse.asMap() = Json.decodeFromString<Map<String, String>>(bodyAsText())

@Serializable
data class Auth(
    val account: String,
    val password: String
)

data class Credentials(
    val deviceId: UUID,
    val auth: Auth,
) {
    val account: String
        get() = auth.account
    val password: String
        get() = auth.password
}

suspend fun HttpResponse.asAuth() = Json.decodeFromString<Auth>(bodyAsText())

fun Auth.toBearerToken(): String {
    val credentials = "$account:$password"
    val encodedCredentials = Base64.getEncoder().encodeToString(credentials.toByteArray())
    return "Basic $encodedCredentials"
}

fun HttpRequestBuilder.addDeviceId(id: UUID) {
    headers {
        append("X-Device-ID", id.toString())
    }
}

fun HttpRequestBuilder.addAuth(auth: Auth) {
    headers {
        append("Authorization", auth.toBearerToken())
    }
}

fun HttpRequestBuilder.addCredentials(credentials: Credentials) {
    addDeviceId(credentials.deviceId)
    addAuth(credentials.auth)
}

suspend fun HttpClient.createDevice(
    deviceId: UUID = UUID.randomUUID(),
    shareCode: String? = null,
): Credentials {
    val credentials = if (shareCode != null) {
        post {
            url {
                takeFrom("/v1/account")
                parameters.append("share", shareCode)
            }
            addDeviceId(deviceId)
        }.asAuth()
    } else {
        post("/v1/account") {
            addDeviceId(deviceId)
        }.asAuth()
    }

    return Credentials(deviceId, credentials)
}

suspend fun HttpClient.createDevice(
    credentials: Credentials,
): Credentials {
    val shareCode = createShareCode(credentials)

    return createDevice(shareCode = shareCode)
}

suspend fun HttpClient.createShareCode(
    credentials: Credentials
): String {
    val shareCode = post("/v1/account/share") {
        addDeviceId(credentials.deviceId)
        addAuth(credentials.auth)
    }.asMap()["code"]!!

    return shareCode
}