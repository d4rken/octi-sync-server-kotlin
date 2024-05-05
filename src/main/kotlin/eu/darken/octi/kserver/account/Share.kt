package eu.darken.octi.kserver.account

import java.time.Instant

data class Share(
    val code: String,
    val accountId: String,
    val createdAt: Instant,
)
