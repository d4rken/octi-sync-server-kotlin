package eu.darken.octi.kserver.account

import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.VERBOSE
import eu.darken.octi.kserver.common.debug.logging.log
import eu.darken.octi.kserver.common.debug.logging.logTag
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ShareRepo @Inject constructor() {

    private val shares = mutableMapOf<String, Share>()
    private val mutex = Mutex()

    suspend fun getShare(shareId: String): Share? = mutex.withLock {
        log(TAG, VERBOSE) { "resolveCode($shareId)" }
        shares[shareId]
    }

    suspend fun consumeShare(shareCode: String): Boolean = mutex.withLock {
        log(TAG, VERBOSE) { "consumeShare($shareCode)" }
        val removed = shares.remove(shareCode)
        log(TAG) { "Share was consumed: $removed" }
        removed != null
    }

    companion object {
        private val TAG = logTag("Account", "Share", "Repo")
    }
}