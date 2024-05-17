package eu.darken.octi.kserver.module

import eu.darken.octi.kserver.device.DeviceId
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

interface Module {

    @Serializable
    data class Info(
        @Contextual @SerialName("id") val id: ModuleId,
        @Contextual @SerialName("source") val source: DeviceId,
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

typealias ModuleId = String
