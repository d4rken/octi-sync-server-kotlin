package eu.darken.octi.kserver.account

import eu.darken.octi.kserver.account.share.ShareRepo
import eu.darken.octi.kserver.common.callInfo
import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.*
import eu.darken.octi.kserver.common.debug.logging.asLog
import eu.darken.octi.kserver.common.debug.logging.log
import eu.darken.octi.kserver.common.debug.logging.logTag
import eu.darken.octi.kserver.common.headerDeviceId
import eu.darken.octi.kserver.common.verifyCaller
import eu.darken.octi.kserver.device.DeviceRepo
import eu.darken.octi.kserver.device.deviceCredentials
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRoute @Inject constructor(
    private val accountRepo: AccountRepo,
    private val deviceRepo: DeviceRepo,
    private val shareRepo: ShareRepo,
) {

    fun setup(rootRoute: Routing) {
        rootRoute.route("/v1/account") {
            post {
                try {
                    create()
                } catch (e: Exception) {
                    log(TAG, ERROR) { "create() failed: ${e.asLog()}" }
                    call.respond(HttpStatusCode.InternalServerError, "Account creation failed")
                }
            }
            delete {
                try {
                    delete()
                } catch (e: Exception) {
                    log(TAG, ERROR) { "delete() failed: ${e.asLog()}" }
                    call.respond(HttpStatusCode.InternalServerError, "Account deletion failed")
                }
            }
        }
    }

    private suspend fun RoutingContext.create() {
        val deviceId = call.headerDeviceId
        val shareCode = call.request.queryParameters["share"]

        log(TAG) { "create($callInfo): deviceId=$deviceId, shareCode=$shareCode" }

        if (deviceId == null) {
            log(TAG, WARN) { "create($callInfo): Missing header ID" }
            call.respond(HttpStatusCode.BadRequest, "X-Device-ID header is missing")
            return
        }

        // Check if this device is already registered
        var device = deviceRepo.getDevice(deviceId)
        if (device != null) {
            log(TAG, WARN) { "create($callInfo): Device is already known: $device" }
            call.respond(HttpStatusCode.BadRequest, "Device is already registered")
            return
        }

        if (deviceCredentials != null) {
            log(TAG, WARN) { "create($callInfo): Credentials were unexpectedly provided" }
            call.respond(HttpStatusCode.BadRequest, "Don't provide credentials during action creation or linking")
            return
        }

        // Try linking device to account
        val share = if (shareCode != null) {
            shareRepo.getShare(shareCode).also {
                if (it == null) {
                    log(TAG, WARN) { "create($callInfo): Could not resolve ShareCode" }
                    call.respond(HttpStatusCode.Forbidden, "Invalid ShareCode")
                }
            } ?: return
        } else {
            null
        }

        val account = if (share != null) {
            if (!shareRepo.consumeShare(shareCode!!)) {
                log(TAG, ERROR) { "create($callInfo): Failed to consume Share" }
                call.respond(HttpStatusCode.InternalServerError, "ShareCode was already consumed")
                return
            }
            log(TAG, INFO) { "create($callInfo): Share was valid, let's add the device" }
            val resolved = accountRepo.getAccount(share.accountId)
            if (resolved == null) {
                log(TAG, ERROR) { "create($callInfo): Account ${share.accountId} disappeared, restoring share" }
                shareRepo.restoreShare(share)
                call.respond(HttpStatusCode.InternalServerError, "Account no longer exists")
                return
            }
            resolved
        } else {
            log(TAG, INFO) { "create($callInfo): Creating new account" }
            accountRepo.createAccount()
        }

        device = try {
            deviceRepo.createDevice(
                deviceId = deviceId,
                account = account,
                version = call.request.headers["User-Agent"],
            )
        } catch (e: Exception) {
            if (share != null) {
                log(TAG, ERROR) { "create($callInfo): Device creation failed, restoring share: ${e.asLog()}" }
                shareRepo.restoreShare(share)
            }
            throw e
        }

        val response = RegisterResponse(
            accountID = device.accountId,
            password = device.password,
        )
        call.respond(response).also {
            log(TAG, INFO) { "create($callInfo): Device registered $device to $account" }
        }
    }

    private suspend fun RoutingContext.delete() {
        val callerDevice = verifyCaller(TAG, deviceRepo) ?: return
        log(TAG, INFO) { "delete(${callInfo}): Deleting account ${callerDevice.accountId}" }

        withContext(NonCancellable) {
            deviceRepo.deleteDevices(callerDevice.accountId)
            shareRepo.removeSharesForAccount(callerDevice.accountId)
            accountRepo.deleteAccounts(listOf(callerDevice.accountId))
        }

        call.respond(HttpStatusCode.OK).also {
            log(TAG, INFO) { "delete($callInfo): Account was deleted: ${callerDevice.accountId}" }
        }
    }

    companion object {
        private val TAG = logTag("Account", "Route")
    }
}