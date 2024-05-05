package eu.darken.octi.kserver.account

import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.VERBOSE
import eu.darken.octi.kserver.common.debug.logging.log
import eu.darken.octi.kserver.common.debug.logging.logTag
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepo @Inject constructor() {

    private val accounts = mutableSetOf<Account>()
    private val mutex = Mutex()
    suspend fun createAccount(): Account = mutex.withLock {
        log(TAG) { "createAccount(): Creating account..." }

        val acc = Account()
        accounts.add(acc)
        acc.also {
            log(TAG) { "createAccount(): Account created: $it" }
        }
    }

    suspend fun getAccount(id: String): Account? = mutex.withLock {
        accounts.singleOrNull { it.id == id }.also {
            log(TAG, VERBOSE) { "getAccount($id) -> $it" }
        }
    }


    companion object {
        private val TAG = logTag("Account", "Repo")
    }
}