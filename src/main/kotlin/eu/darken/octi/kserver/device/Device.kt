package eu.darken.octi.kserver.device

import eu.darken.octi.kserver.account.AccountId
import eu.darken.octi.kserver.common.generateRandomKey
import kotlinx.coroutines.sync.Mutex
import java.security.MessageDigest
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.nio.file.Path
import java.time.Instant
import java.util.*


data class Device(
    val data: Data,
    val path: Path,
    val accountId: AccountId,
    val sync: Mutex = Mutex(),
) {
    fun isAuthorized(credentials: DeviceCredentials): Boolean {
        return accountId == credentials.accountId &&
            MessageDigest.isEqual(
                data.password.toByteArray(),
                credentials.devicePassword.toByteArray()
            )
    }

    val id: DeviceId
        get() = data.id

    val password: String
        get() = data.password

    val version: String?
        get() = data.version

    val lastSeen: Instant
        get() = data.lastSeen

    @Serializable
    data class Data(
        @Contextual val id: DeviceId,
        val password: String = generateRandomKey(),
        val version: String?,
        @Contextual val addedAt: Instant = Instant.now(),
        @Contextual val lastSeen: Instant = Instant.now(),
    ) {
        override fun toString(): String = "Device.Data(added=$addedAt, seen=$lastSeen, $id, ${password.take(8)}...)"
    }
}

typealias DeviceId = UUID
