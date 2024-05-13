package eu.darken.octi.kserver.share

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class Share(
    val code: String,
    val accountId: String,
    @Contextual val createdAt: Instant,
)