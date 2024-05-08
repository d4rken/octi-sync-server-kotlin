package eu.darken.octi.kserver.device

import eu.darken.octi.kserver.common.generateRandomKey
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.io.File
import java.time.Instant
import java.util.*


data class Device(
    val data: Data,
    val path: File,
    val sync: Mutex = Mutex(),
) {
    fun isAuthorized(credentials: DeviceCredentials): Boolean {
        return data.accountId == credentials.accountId && data.password == credentials.devicePassword
    }

    val id: String
        get() = data.id

    val accountId: String
        get() = data.accountId

    val password: String
        get() = data.password

    @Serializable
    data class Data(
        val id: String = UUID.randomUUID().toString(),
        val accountId: String,
        val password: String = generateRandomKey(),
        val label: String,
        @Contextual val addedAt: Instant = Instant.now(),
    )
}
