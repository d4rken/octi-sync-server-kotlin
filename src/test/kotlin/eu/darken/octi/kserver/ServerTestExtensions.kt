package eu.darken.octi.kserver

import eu.darken.octi.kserver.BaseServerTest.TestEnv
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

suspend fun TestEnv.createDeviceRaw(
    deviceId: UUID = UUID.randomUUID(),
    shareCode: String? = null,
): HttpResponse = this.http.run {
    if (shareCode != null) {
        post {
            url {
                takeFrom("/v1/account")
                parameters.append("share", shareCode)
            }
            addDeviceId(deviceId)
        }
    } else {
        post("/v1/account") {
            addDeviceId(deviceId)
        }
    }
}

suspend fun TestEnv.createDevice(
    deviceId: UUID = UUID.randomUUID(),
    shareCode: String? = null,
): Credentials {
    val credentials = createDeviceRaw(deviceId, shareCode).asAuth()
    return Credentials(deviceId, credentials)
}

suspend fun TestEnv.createDevice(
    credentials: Credentials,
): Credentials {
    val shareCode = createShareCode(credentials)

    return createDevice(shareCode = shareCode)
}

suspend fun TestEnv.createShareCode(
    credentials: Credentials
): String = http.run {
    val shareCode = post("/v1/account/share") {
        addDeviceId(credentials.deviceId)
        addAuth(credentials.auth)
    }.asMap()["code"]!!

    return shareCode
}

fun HttpRequestBuilder.targetModule(moduleId: String, device: UUID?) {
    url {
        takeFrom("/v1/modules/$moduleId")
        if (device != null) parameters.append("device-id", device.toString())
    }
}

suspend fun TestEnv.readModule(
    creds: Credentials,
    moduleId: String,
    deviceId: UUID? = creds.deviceId,
) = http.get {
    targetModule(moduleId, deviceId)
    addCredentials(creds)
}

suspend fun TestEnv.writeModule(
    creds: Credentials,
    moduleId: String,
    deviceId: UUID? = creds.deviceId,
    body: String,
) = http.post {
    targetModule(moduleId, deviceId)
    addCredentials(creds)
    contentType(ContentType.Application.OctetStream)
    setBody(body)
}

suspend fun TestEnv.deleteModule(
    creds: Credentials,
    moduleId: String,
    deviceId: UUID? = creds.deviceId,
) = http.delete {
    targetModule(moduleId, deviceId)
    addCredentials(creds)
}