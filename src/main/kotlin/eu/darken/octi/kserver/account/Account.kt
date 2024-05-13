package eu.darken.octi.kserver.account

import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.io.File
import java.time.Instant
import java.util.*

data class Account(
    val data: Data,
    val path: File,
    val sync: Mutex = Mutex(),
) {
    val id: AccountId
        get() = data.id

    @Serializable
    data class Data(
        @Contextual val id: AccountId = UUID.randomUUID(),
        @Contextual val createdAt: Instant = Instant.now(),
    )
}

typealias AccountId = UUID