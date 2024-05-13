package eu.darken.octi.kserver.account

import eu.darken.octi.kserver.account.share.ShareRepo
import eu.darken.octi.kserver.common.callInfo
import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.*
import eu.darken.octi.kserver.common.debug.logging.asLog
import eu.darken.octi.kserver.common.debug.logging.log
import eu.darken.octi.kserver.common.debug.logging.logTag
import eu.darken.octi.kserver.common.headerDeviceId
import eu.darken.octi.kserver.common.verifyAuth
import eu.darken.octi.kserver.device.DeviceRepo
import eu.darken.octi.kserver.device.deviceCredentials
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRoute @Inject constructor(
    private val accountRepo: AccountRepo,
    private val deviceRepo: DeviceRepo,
    private val shareRepo: ShareRepo,
) {

    fun setup(rootRoute: RootRoute) {
        rootRoute.route("/v1/account") {
            this.post {
                try {
                    handleCreate()
                } catch (e: Exception) {
                    log(TAG, ERROR) { "create() failed: ${e.asLog()}" }
                    call.respond(HttpStatusCode.InternalServerError, "Account creation failed")
                }
            }
            delete {
                try {
                    handleDelete()
                } catch (e: Exception) {
                    log(TAG, ERROR) { "delete() failed: ${e.asLog()}" }
                    call.respond(HttpStatusCode.InternalServerError, "Account deletion failed")
                }
            }
        }
    }

    private suspend fun RoutingContext.handleCreate() {
        val deviceId = call.headerDeviceId
        val shareCode = call.request.queryParameters["share"]

        log(TAG) { "create($callInfo): deviceId=$deviceId, shareCode=$shareCode" }

        if (deviceId == null) {
            log(TAG, WARN) { "create($callInfo): Missing header ID" }
            call.respond(HttpStatusCode.BadRequest, "X-Device-ID header is missing")
            return
        }

        val credentials = this.deviceCredentials
        log(TAG, VERBOSE) { "create($callInfo): credentials=$credentials" }

        // At this point there is no device registered with $deviceId yet
        var account = credentials?.let { accountRepo.getAccount(it.accountId) }
        if (account != null) log(TAG) { "create($callInfo): Found matching account: $account" }

        // Can we add this device to an existing account?
        if (shareCode != null) {
            if (account == null) {
                log(TAG, WARN) { "create($callInfo): Can't use shareId, no account provided" }
                call.respond(HttpStatusCode.BadRequest, "Account is missing")
                return
            }

            val share = shareRepo.getShare(shareCode)
            if (share == null) {
                log(TAG, WARN) { "create($callInfo): Could not resolve ShareCode" }
                call.respond(HttpStatusCode.Forbidden, "Invalid ShareCode")
                return
            }

            if (share.accountId != credentials?.accountId) {
                log(TAG, WARN) { "create($callInfo): Share invalid, account ID mismatch" }
                call.respond(HttpStatusCode.Forbidden, "Invalid account")
                return
            }

            if (shareRepo.consumeShare(shareCode)) {
                log(TAG, INFO) { "create($callInfo): Share was valid and matches account, let's add the device" }
            } else {
                log(TAG, ERROR) { "create($callInfo): Failed to consume Share" }
                call.respond(HttpStatusCode.InternalServerError, "ShareCode was already consumed")
                return
            }
        } else {
            if (credentials != null) {
                log(TAG, WARN) { "create($callInfo): Credentials provided but no ShareCode" }
                call.respond(HttpStatusCode.BadRequest, "ShareCode required if credentials are provided")
                return
            }

            log(TAG, INFO) { "create($callInfo): No ShareCode and Account does not exist, create one" }
            account = accountRepo.createAccount()
        }

        // Check if this device is already registered
        var device = deviceRepo.getDevice(deviceId)
        if (device != null) {
            log(TAG, WARN) { "create($callInfo): Device is already known: $device" }
            call.respond(HttpStatusCode.BadRequest, "Device is already registered")
            return
        }

        device = deviceRepo.createDevice(
            account = account,
            label = call.request.headers["User-Agent"] ?: ""
        )

        val response = RegisterResponse(
            accountID = device.accountId,
            password = device.password,
        )
        call.respond(response).also {
            log(TAG, INFO) { "create($callInfo): Device registered $device to $account" }
        }
    }

    private suspend fun RoutingContext.handleDelete() {
        val device = verifyAuth(TAG, deviceRepo) ?: return

        log(TAG, INFO) { "delete(${callInfo}): User is authorized, deleting account..." }
        accountRepo.deleteAccount(device.accountId)

        call.respond(HttpStatusCode.OK).also {
            log(TAG, INFO) { "delete($callInfo): Account was deleted: ${device.accountId}" }
        }
    }

    companion object {
        private val TAG = logTag("Account", "Route")
    }
}