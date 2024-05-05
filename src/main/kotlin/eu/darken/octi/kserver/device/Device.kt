package eu.darken.octi.kserver.device

import eu.darken.octi.kserver.common.generateRandomKey
import java.util.*

data class Device(
    val id: String = UUID.randomUUID().toString(),
    val accountId: String,
    val password: String = generateRandomKey(),
    val label: String,
)
