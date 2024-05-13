package eu.darken.octi.kserver.account.share

import eu.darken.octi.kserver.account.AccountRepo
import eu.darken.octi.kserver.common.callInfo
import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.INFO
import eu.darken.octi.kserver.common.debug.logging.log
import eu.darken.octi.kserver.common.debug.logging.logTag
import eu.darken.octi.kserver.common.verifyAuth
import eu.darken.octi.kserver.device.DeviceRepo
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareRoute @Inject constructor(
    private val accountRepo: AccountRepo,
    private val deviceRepo: DeviceRepo,
    private val shareRepo: ShareRepo,
) {

    fun setup(rootRoute: RootRoute) {
        rootRoute.post("/v1/account/share") {
            try {
                handleShareCreation()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Share code creation failed")
            }
        }
    }

    private suspend fun RoutingContext.handleShareCreation() {
        val device = verifyAuth(TAG, deviceRepo) ?: return

        val account = accountRepo.getAccount(device.accountId)
            ?: throw IllegalStateException("Account not found for $device")

        val share = shareRepo.createShare(account)

        call.respond(share).also {
            log(TAG, INFO) { "createShare($callInfo): Share created: $share" }
        }
    }

    companion object {
        private val TAG = logTag("Account", "Share", "Route")
    }
}