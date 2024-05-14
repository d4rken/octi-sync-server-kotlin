package eu.darken.octi.kserver.account.share

import eu.darken.octi.kserver.common.generateRandomKey
import kotlinx.serialization.Serializable

@Serializable
data class ShareResponse(
    val code: ShareCode = generateRandomKey(),
)