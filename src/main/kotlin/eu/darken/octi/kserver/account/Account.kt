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
    val id: String
        get() = data.id

    @Serializable
    data class Data(
        val id: String = UUID.randomUUID().toString(),
        @Contextual val createdAt: Instant = Instant.now(),
    )
}