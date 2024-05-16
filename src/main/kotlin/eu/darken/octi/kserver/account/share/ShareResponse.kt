package eu.darken.octi.kserver.account.share

import eu.darken.octi.kserver.common.generateRandomKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShareResponse(
    @SerialName("code") val code: ShareCode = generateRandomKey(),
)