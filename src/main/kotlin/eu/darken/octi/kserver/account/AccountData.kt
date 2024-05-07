package eu.darken.octi.kserver.account

import java.time.Instant
import java.util.*

data class AccountData(
    val id: String = UUID.randomUUID().toString(),
    val createdAt: Instant = Instant.now(),
)