package eu.darken.octi.kserver.account

import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.nio.file.Path
import java.time.Instant
import java.util.*

data class Account(
    val data: Data,
    val path: Path,
    val sync: Mutex = Mutex(),
) {
    val id: AccountId
        get() = data.id

    val createdAt: Instant
        get() = data.createdAt

    @Serializable
    data class Data(
        @Contextual val id: AccountId = UUID.randomUUID(),
        @Contextual val createdAt: Instant = Instant.now(),
    ) {
        override fun toString(): String = "Account.Data(created=$createdAt, $id)"
    }
}

typealias AccountId = UUID