package eu.darken.octi.kserver.account.share

import eu.darken.octi.kserver.App
import eu.darken.octi.kserver.account.Account
import eu.darken.octi.kserver.account.AccountId
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
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


@Singleton
class ShareRepo @Inject constructor(
    appScope: AppScope,
    private val serializer: Json,
    private val accountsRepo: AccountRepo,
    private val config: App.Config,
) {

    private val shares = ConcurrentHashMap<ShareId, Share>()
    private val mutex = Mutex()

    init {
        runBlocking {
            accountsRepo.getAccounts()
                .asSequence()
                .mapNotNull { account ->
                    try {
                        Files
                            .newDirectoryStream(account.path.resolve(SHARES_DIR))
                            .map { account to it }
                            .toList().also {
                                log(TAG) { "Loading ${it.size} shares from account with ID=${account.id}" }
                            }
                    } catch (e: IOException) {
                        log(TAG, ERROR) { "Failed to list shares for $account" }
                        null
                    }
                }
                .flatten()
                .forEach { (account, path) ->
                    val data: Share.Data = try {
                        serializer.decodeFromString(path.readText())
                    } catch (e: IOException) {
                        log(TAG, ERROR) { "Failed to read $path: ${e.asLog()}" }
                        return@forEach
                    }
                    log(TAG) { "Share info loaded: $data" }
                    shares[data.id] = Share(
                        data = data,
                        path = path,
                        accountId = account.id,
                    )
                }
            log(TAG, INFO) { "${shares.size} shares loaded into memory" }
        }

        appScope.launch(Dispatchers.IO) {
            val expirationTime = config.account.shareExpirationTime

            while (currentCoroutineContext().isActive) {
                log(TAG) { "Checking for expired shares..." }
                val now = Instant.now()
                val expiredShares = shares.filterValues { share ->
                    Duration.between(share.createdAt, now) > expirationTime
                }
                if (expiredShares.isNotEmpty()) {
                    log(TAG, INFO) { "Deleting ${expiredShares.size} expired shares" }
                    removeShares(expiredShares.map { it.value.id })
                }
                delay(expirationTime.toMillis() / 2)
            }
        }

        appScope.launch(Dispatchers.IO) {
            delay(15.seconds)
            while (currentCoroutineContext().isActive) {
                log(TAG) { "Checking for stale share data..." }
                val staleShares = shares.values.filter { !it.path.exists() }
                if (staleShares.isNotEmpty()) {
                    log(TAG, INFO) { "Removing ${staleShares.size} stale shares" }
                    removeShares(staleShares.map { it.id })
                }
                delay(10.minutes)
            }
        }
    }

    suspend fun createShare(account: Account): Share = mutex.withLock {
        log(TAG) { "createShare(${account.id}): Creating share..." }

        val data = Share.Data()
        val share = Share(
            data = data,
            path = account.path.resolve("shares/${data.id}.json"),
            accountId = account.id,
        )
        if (shares.containsKey(share.id)) throw IllegalStateException("Share ID collision???")

        share.path.run {
            if (!parent.exists()) {
                Files.createDirectory(parent)
                log(TAG) { "createShare(${account.id}): Parent created for $this" }
            }
            writeText(serializer.encodeToString(share.data))
            log(TAG, VERBOSE) { "createShare(${account.id}): Written to $this" }
        }
        shares[share.id] = share
        share.also { log(TAG) { "createShare(${account.id}): Share created created: $it" } }
    }

    suspend fun getShare(code: ShareCode): Share? {
        log(TAG, VERBOSE) { "getShare($code)" }
        return shares.values.find { it.code == code }
    }

    suspend fun consumeShare(code: ShareCode): Boolean {
        log(TAG, VERBOSE) { "consumeShare($code)" }
        val share = getShare(code) ?: return false
        removeShares(listOf(share.id))
        log(TAG) { "Share was consumed: $share" }
        return true
    }

    suspend fun removeShares(ids: Collection<ShareId>) = mutex.withLock {
        log(TAG) { "removeShares($ids)..." }
        val toRemove = ids.mapNotNull { shares.remove(it) }
        log(TAG) { "removeShares($ids): Deleting ${toRemove.size} shares" }
        toRemove.forEach {
            it.path.deleteIfExists()
            log(TAG, VERBOSE) { "removeShares($ids): Share deleted $it" }
        }
    }

    suspend fun removeSharesForAccount(accountId: AccountId) {
        log(TAG) { "removeSharesForAccount($accountId)..." }
        val toRemove = shares.filter { it.value.accountId == accountId }.map { it.key }
        log(TAG) { "removeSharesForAccount($accountId): Deleting ${toRemove.size} shares" }
        removeShares(toRemove)
    }

    companion object {
        private const val SHARES_DIR = "shares"
        private val TAG = logTag("Account", "Share", "Repo")
    }
}