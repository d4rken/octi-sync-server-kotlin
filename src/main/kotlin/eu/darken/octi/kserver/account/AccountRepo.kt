package eu.darken.octi.kserver.account

import eu.darken.octi.kserver.Application
import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.*
import eu.darken.octi.kserver.common.debug.logging.asLog
import eu.darken.octi.kserver.common.debug.logging.log
import eu.darken.octi.kserver.common.debug.logging.logTag
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepo @Inject constructor(
    private val serializer: Json,
) {
    private val accountsPath = File(Application.dataPath, "accounts").apply {
        if (mkdirs()) log(TAG) { "Created $this" }
    }
    private val accounts = mutableMapOf<String, Account>()
    private val mutex = Mutex()

    init {
        val accountDirs = accountsPath.listFiles()
        requireNotNull(accountDirs) { "Could not load account directory" }
        log(TAG, INFO) { "${accountDirs.size} account dirs found" }

        accountDirs
            .filter {
                if (it.isDirectory) {
                    true
                } else {
                    log(TAG, WARN) { "Not a directory: $it" }
                    false
                }
            }
            .forEach { accDir ->
                val accData = try {
                    serializer.decodeFromString<Account.Data>(File(accDir, ACC_FILENAME).readText())
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

    suspend fun createAccount(): Account = mutex.withLock {
        log(TAG) { "createAccount(): Creating account..." }

        val accData = Account.Data()
        if (accounts.containsKey(accData.id)) throw IllegalStateException("Account ID collision???")

        log(TAG) { "createAccount(): Account created: $accData" }
        val account = Account(
            data = accData,
            path = File(accountsPath, accData.id)
        )

        account.path.run {
            if (mkdirs()) log(TAG) { "createAccount(): Dirs created: $this" }
            File(this, ACC_FILENAME).writeText(serializer.encodeToString(accData))
            log(TAG, VERBOSE) { "Account written to $this" }
        }
        accounts[accData.id] = account
        account
    }

    suspend fun getAccount(id: String): Account? = mutex.withLock {
        accounts[id].also {
            log(TAG, VERBOSE) { "getAccount($id) -> $it" }
        }
    }

    suspend fun deleteAccount(id: String) = mutex.withLock {
        log(TAG, INFO) { "deleteAccount($id)" }
        val account = accounts.remove(id) ?: throw IllegalArgumentException("Unknown account")
        val accountDir = account.path
        if (!accountDir.deleteRecursively()) {
            log(TAG, ERROR) { "Failed to delete account directory: $accountDir" }
            val accountConfig = File(account.path, ACC_FILENAME)
            if (accountConfig.exists() || accountConfig.delete()) {
                log(TAG, INFO) { "Deleted account file, will clean up on next restart." }
            }
        }
    }

    suspend fun getAllAccounts(): List<Account> = mutex.withLock {
        return accounts.values.toList()
    }

    companion object {
        private const val ACC_FILENAME = "account.json"
        private val TAG = logTag("Account", "Repo")
    }
}