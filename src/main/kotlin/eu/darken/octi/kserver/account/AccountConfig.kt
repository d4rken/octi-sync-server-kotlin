package eu.darken.octi.kserver.account

import java.time.Duration

data class AccountConfig(
    val shareExpirationTime: Duration = Duration.ofMinutes(10),
    val shareGCInterval: Duration = Duration.ofMinutes(10),
)