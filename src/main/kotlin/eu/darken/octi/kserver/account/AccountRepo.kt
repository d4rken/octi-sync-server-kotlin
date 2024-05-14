@file:OptIn(ExperimentalPathApi::class)

package eu.darken.octi.kserver.account

import eu.darken.octi.kserver.Application
import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.*
import eu.darken.octi.kserver.common.debug.logging.asLog
import eu.darken.octi.kserver.common.debug.logging.log
import eu.darken.octi.kserver.common.debug.logging.logTag
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.nio.file.Files
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
@Singleton
class AccountRepo @Inject constructor(
    private val serializer: Json,
) {
    private val accountsPath = Application.dataPath.resolve("accounts").apply {
        if (!exists()) {
            Files.createDirectories(this)
            log(TAG) { "Created $this" }
        }
    }
    private val accounts = mutableMapOf<UUID, Account>()
    private val mutex = Mutex()

    init {
        runBlocking {
            Files.newDirectoryStream(accountsPath)
                .filter {
                    if (it.isDirectory()) {
                        true
                    } else {
                        log(TAG, WARN) { "Not a directory: $it" }
                        false
                    }
                }
                .forEach { accDir ->
                    val accData = try {
                        serializer.decodeFromString<Account.Data>(accDir.resolve(ACC_FILENAME).readText())
                    } catch (e: IOException) {
                        log(TAG, ERROR) { "Failed to read $accDir: ${e.asLog()}" }
                        return@forEach
                    }
                    log(TAG) { "Account info loaded: $accData" }
                    accounts[accData.id] = Account(
                        data = accData,
                        path = accDir,
                    )
                }

            log(TAG, INFO) { "${accounts.size} accounts loaded into memory" }
        }
    }

    suspend fun createAccount(): Account = mutex.withLock {
        log(TAG) { "createAccount(): Creating account..." }

        val accData = Account.Data()
        if (accounts.containsKey(accData.id)) throw IllegalStateException("Account ID collision???")

        val account = Account(
            data = accData,
            path = accountsPath.resolve(accData.id.toString()),
        )

        account.path.run {
            if (!exists()) {
                createDirectory()
                log(TAG) { "createAccount(): Dirs created: $this" }
            }
            resolve(ACC_FILENAME).writeText(serializer.encodeToString(accData))
            log(TAG, VERBOSE) { "createAccount(): Account written to $this" }
        }
        accounts[accData.id] = account
        account.also { log(TAG) { "createAccount(): Account created: $accData" } }
    }

    suspend fun getAccount(id: UUID): Account? = mutex.withLock {
        accounts[id].also {
            log(TAG, VERBOSE) { "getAccount($id) -> $it" }
        }
    }

    suspend fun deleteAccount(id: UUID) = mutex.withLock {
        log(TAG) { "deleteAccount($id)..." }
        val account = accounts.remove(id) ?: throw IllegalArgumentException("Unknown account")
        val accountDir = account.path
        val deleted = try {
            accountDir.deleteRecursively()
            true
        } catch (e: IOException) {
            log(TAG, ERROR) { "Failed to delete account directory: $accountDir: ${e.asLog()}" }
            false
        }
        if (!deleted) {
            val accountConfig = account.path.resolve(ACC_FILENAME)
            if (accountConfig.deleteIfExists()) {
                log(TAG, WARN) { "Deleted account file, will clean up on next restart." }
            }
        }
        log(TAG) { "deleteAccount($id): Account deleted: $account" }
    }

    suspend fun getAllAccounts(): List<Account> = mutex.withLock {
        return accounts.values.toList()
    }

    companion object {
        private const val ACC_FILENAME = "account.json"
        private val TAG = logTag("Account", "Repo")
    }
}