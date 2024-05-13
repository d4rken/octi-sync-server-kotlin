package eu.darken.octi.kserver.account.share

import eu.darken.octi.kserver.account.AccountId
import eu.darken.octi.kserver.common.generateRandomKey
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.*

@Serializable
data class Share(
    @Contextual val accountId: AccountId,
    @Contextual val id: ShareId = UUID.randomUUID(),
    val code: ShareCode = generateRandomKey(),
    @Contextual val createdAt: Instant = Instant.now(),
)

typealias ShareCode = String
typealias ShareId = UUID