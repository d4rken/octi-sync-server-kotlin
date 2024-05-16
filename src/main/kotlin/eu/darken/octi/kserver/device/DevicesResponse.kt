package eu.darken.octi.kserver.device

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DevicesResponse(
    @SerialName("devices") val devices: List<Device>,
) {
    @Serializable
    data class Device(
        @Contextual @SerialName("id") val id: DeviceId,
        @SerialName("label") val label: String?,
        @SerialName("version") val version: String?,
    )
}