package eu.darken.octi.kserver.module

import eu.darken.octi.kserver.account.AccountId
import eu.darken.octi.kserver.device.Device
import eu.darken.octi.kserver.device.DeviceId
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.nio.file.Path
import java.time.Instant
import java.util.*

data class Module(
    val accountId: AccountId,
    val deviceId: DeviceId,
    val meta: Meta,
    val path: Path,
    val sync: Mutex = Mutex(),
) {

    fun requireSameAccount(device: Device) {
        if (device.accountId != accountId) throw IllegalArgumentException("$this does not belong to $device")
    }

    val id: ModuleId
        get() = meta.id

    @Serializable
    data class Meta(
        @Contextual @SerialName("id") val id: ModuleId = UUID.randomUUID(),
        @Contextual @SerialName("modifiedAt") val modifiedAt: Instant = Instant.now(),
    )

    data class Read(
        val modifiedAt: Instant? = null,
        val payload: ByteArray = ByteArray(0),
    ) {
        val size: Int
            get() = payload.size
    }

    data class Write(
        val payload: ByteArray,
    ) {
        val size: Int
            get() = payload.size
    }
}

typealias ModuleId = UUID
