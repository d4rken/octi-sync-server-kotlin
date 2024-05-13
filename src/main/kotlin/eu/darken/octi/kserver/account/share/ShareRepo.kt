package eu.darken.octi.kserver.account.share

import eu.darken.octi.kserver.account.Account
import eu.darken.octi.kserver.account.AccountRepo
import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.*
import eu.darken.octi.kserver.common.debug.logging.asLog
import eu.darken.octi.kserver.common.debug.logging.log
import eu.darken.octi.kserver.common.debug.logging.logTag
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ShareRepo @Inject constructor(
    private val serializer: Json,
    private val accountsRepo: AccountRepo,
) {

    private val shares = mutableMapOf<ShareCode, Share>()
    private val mutex = Mutex()

    init {
        runBlocking {
            accountsRepo.getAllAccounts()
                .asSequence()
                .mapNotNull { account ->
                    try {
                        File(account.path, SHARES_DIR).listFiles()?.toList().also {
                            log(TAG) { "Loading ${it?.size} shares from $account" }
                        }
                    } catch (e: IOException) {
                        log(TAG, ERROR) { "Failed to list shares for $account" }
                        null
                    }
                }
                .flatten()
                .forEach { shareFile ->
                    val share = try {
                        serializer.decodeFromString<Share>(shareFile.readText())
                    } catch (e: IOException) {
                        log(TAG, ERROR) { "Failed to read $shareFile: ${e.asLog()}" }
                        return@forEach
                    }
                    log(TAG) { "Share info loaded: $share" }
                    shares[share.code] = share
                }
            log(TAG, INFO) { "${shares.size} shares loaded into memory" }
        }
    }

    private fun Share.getPath(account: Account): File = File(account.path, "shares/$id.json")

    suspend fun createShare(account: Account): Share = mutex.withLock {
        log(TAG) { "createShare(${account.id}): Creating share..." }

        val share = Share(accountId = account.id)
        if (shares.containsKey(share.code)) throw IllegalStateException("Share ID collision???")

        share.getPath(account).run {
            if (parentFile.mkdirs()) log(TAG) { "createShare(${account.id}): Parents created: $this" }
            writeText(serializer.encodeToString(share))
            log(TAG, VERBOSE) { "createShare(${account.id}): Written to $this" }
        }
        shares[share.code] = share
        share.also {
            log(TAG, INFO) { "createShare(${account.id}): Share created created: $it" }
        }
    }

    suspend fun getShare(code: ShareCode): Share? = mutex.withLock {
        log(TAG, VERBOSE) { "getShare($code)" }
        shares[code]
    }

    suspend fun consumeShare(shareCode: ShareCode): Boolean = mutex.withLock {
        log(TAG, VERBOSE) { "consumeShare($shareCode)" }
        val removed = shares.remove(shareCode)
        log(TAG) { "Share was consumed: $removed" }
        removed != null
    }

    companion object {
        private const val SHARES_DIR = "shares"
        private val TAG = logTag("Account", "Share", "Repo")
    }
}