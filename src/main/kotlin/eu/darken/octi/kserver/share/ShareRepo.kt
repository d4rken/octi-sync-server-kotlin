package eu.darken.octi.kserver.share

import eu.darken.octi.kserver.account.AccountRepo
import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.*
import eu.darken.octi.kserver.common.debug.logging.asLog
import eu.darken.octi.kserver.common.debug.logging.log
import eu.darken.octi.kserver.common.debug.logging.logTag
import eu.darken.octi.kserver.device.Device
import eu.darken.octi.kserver.device.DeviceRepo
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    private val shares = mutableMapOf<String, Share>()
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

    suspend fun getShare(shareId: String): Share? = mutex.withLock {
        log(TAG, VERBOSE) { "getShare($shareId)" }
        shares[shareId]
    }

    suspend fun consumeShare(shareCode: String): Boolean = mutex.withLock {
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