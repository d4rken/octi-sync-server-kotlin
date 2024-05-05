package eu.darken.octi.kserver.account

import eu.darken.octi.kserver.common.debug.logging.logTag
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ShareRepo @Inject constructor() {
    suspend fun resolveCode(shareId: String): Share? {
        return null
    }

    companion object {
        private val TAG = logTag("Account", "Share", "Repo")
    }
}