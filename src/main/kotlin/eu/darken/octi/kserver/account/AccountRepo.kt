package eu.darken.octi.kserver.account

import eu.darken.octi.kserver.Application
import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.*
import eu.darken.octi.kserver.common.debug.logging.log
import eu.darken.octi.kserver.common.debug.logging.logTag
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepo @Inject constructor(
) {
    private val mutex = Mutex()
    private val accounts = mutableMapOf<String, Account>()
    private val accountsPath = File(Application.dataPath, "accounts")
    private val serializer = Json {
        ignoreUnknownKeys = true
    }


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
            .map { File(it, ACC_FILENAME) }
            .forEach { accFile ->
                val account: Account = accFile.readAccount()
                log(TAG) { "Account info loaded: $account" }
                accounts[account.id] = account
            }
    }

    private fun File.readAccount(): Account {
        return serializer.decodeFromString<Account>(this.readText()).also {
            log(TAG, VERBOSE) { "Account read: $it" }
        }
    }

    private fun Account.writeToFile() {
        val accountDir = File(accountsPath, id).apply { mkdirs() }
        File(accountDir, ACC_FILENAME).writeText(serializer.encodeToString(this)).also {
            log(TAG, VERBOSE) { "Account written: $it" }
        }
    }

    suspend fun createAccount(): Account = mutex.withLock {
        log(TAG) { "createAccount(): Creating account..." }

        val acc = Account()
        if (accounts.containsKey(acc.id)) throw IllegalStateException("Account ID collision???")
        accounts[acc.id] = acc
        acc.writeToFile()

        acc.also { log(TAG) { "createAccount(): Account created: $it" } }
    }

    suspend fun getAccount(id: String): Account? = mutex.withLock {
        accounts.singleOrNull { it.id == id }.also {
            log(TAG, VERBOSE) { "getAccount($id) -> $it" }
        }
    }


    companion object {
        private const val ACC_FILENAME = "accounts.json"
        private val TAG = logTag("Account", "Repo")
    }
}