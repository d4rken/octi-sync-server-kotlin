package eu.darken.octi.kserver.account.share

import eu.darken.octi.kserver.account.AccountId
import eu.darken.octi.kserver.common.generateRandomKey
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.nio.file.Path
import java.time.Instant
import java.util.*


data class Share(
    val data: Data,
    val path: Path,
) {

    val accountId: AccountId
        get() = data.accountId

    val code: ShareCode
        get() = data.code

    val createdAt: Instant
        get() = data.createdAt

    @Serializable
    data class Data(
        @Contextual val createdAt: Instant = Instant.now(),
        @Contextual val accountId: AccountId,
        @Contextual val id: ShareId = UUID.randomUUID(),
        val code: ShareCode = generateRandomKey(),
    )
}


typealias ShareCode = String
typealias ShareId = UUID