package eu.darken.octi.kserver.account.share

import eu.darken.octi.kserver.account.Account
import eu.darken.octi.kserver.account.AccountRepo
import eu.darken.octi.kserver.common.AppScope
import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.*
import eu.darken.octi.kserver.common.debug.logging.asLog
import eu.darken.octi.kserver.common.debug.logging.log
import eu.darken.octi.kserver.common.debug.logging.logTag
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.path.deleteExisting
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.seconds


@Singleton
class ShareRepo @Inject constructor(
    private val serializer: Json,
    private val accountsRepo: AccountRepo,
    private val appScope: AppScope,
) {

    private val shares = mutableMapOf<ShareCode, Share>()
    private val mutex = Mutex()

    init {
        runBlocking {
            accountsRepo.getAllAccounts()
                .asSequence()
                .mapNotNull { account ->
                    try {
                        val path = account.path.resolve(SHARES_DIR)
                        Files.newDirectoryStream(path).toList().also {
                            log(TAG) { "Loading ${it.size} shares from ${account.id}" }
                        }
                    } catch (e: IOException) {
                        log(TAG, ERROR) { "Failed to list shares for $account" }
                        null
                    }
                }
                .flatten()
                .forEach { path ->
                    val data: Share.Data = try {
                        serializer.decodeFromString(path.readText())
                    } catch (e: IOException) {
                        log(TAG, ERROR) { "Failed to read $path: ${e.asLog()}" }
                        return@forEach
                    }
                    log(TAG) { "Share info loaded: $data" }
                    shares[data.code] = Share(
                        data = data,
                        path = path
                    )
                }
            log(TAG, INFO) { "${shares.size} shares loaded into memory" }
        }
        appScope.launch(Dispatchers.IO) {
            while (currentCoroutineContext().isActive) {
                log(TAG) { "Checking for expired shares..." }
                val now = Instant.now()
                mutex.withLock {
                    shares.values
                        .filter {
                            // TODO increase
                            Duration.between(it.createdAt, now) > Duration.ofMinutes(1)
                        }
                        .also { log(TAG, INFO) { "There are ${it.size} expired shares" } }
                        .forEach {
                            try {
                                it.path.deleteExisting()
                                shares.remove(it.code)
                                log(TAG) { "Deleted expired share: $it" }
                            } catch (e: IOException) {
                                log(TAG, ERROR) { "Failed to delete expired share $it: ${e.asLog()}" }
                            }
                        }
                }

                // TODO increase
                delay(10.seconds)
            }
        }
        appScope.launch(Dispatchers.IO) {
            while (currentCoroutineContext().isActive) {
                log(TAG) { "Checking for stale share data..." }
                mutex.withLock {
                    shares.values
                        .filter { !it.path.exists() }
                        .also { log(TAG, INFO) { "There are ${it.size} stale shares" } }
                        .forEach {
                            shares.remove(it.code)
                            log(TAG) { "Removed stale share for ${it.accountId}: $it" }
                        }
                }
                // TODO increase
                delay(10.seconds)
            }
        }
    }


    suspend fun createShare(account: Account): Share = mutex.withLock {
        log(TAG) { "createShare(${account.id}): Creating share..." }

        val data = Share.Data(
            accountId = account.id
        )
        val share = Share(
            data = data,
            path = account.path.resolve("shares/${data.id}.json")
        )
        if (shares.containsKey(share.code)) throw IllegalStateException("Share ID collision???")

        share.path.run {
            if (!parent.exists()) {
                Files.createDirectory(parent)
                log(TAG) { "createShare(${account.id}): Parent created for $this" }
            }
            writeText(serializer.encodeToString(share.data))
            log(TAG, VERBOSE) { "createShare(${account.id}): Written to $this" }
        }
        shares[share.code] = share
        share.also { log(TAG) { "createShare(${account.id}): Share created created: $it" } }
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