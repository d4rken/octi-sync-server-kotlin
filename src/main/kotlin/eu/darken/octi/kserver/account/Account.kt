package eu.darken.octi.kserver.account

import kotlinx.coroutines.sync.Mutex

data class Account(
    val data: AccountData,
    val sync: Mutex = Mutex(),
) {
}