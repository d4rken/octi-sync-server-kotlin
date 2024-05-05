package eu.darken.octi.kserver.account

import eu.darken.octi.kserver.common.callInfo
import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.*
import eu.darken.octi.kserver.common.debug.logging.log
import eu.darken.octi.kserver.common.debug.logging.logTag
import eu.darken.octi.kserver.device.DeviceRepo
import eu.darken.octi.kserver.device.deviceCredentials
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRoute @Inject constructor(
    private val accountRepo: AccountRepo,
    private val deviceRepo: DeviceRepo,
    private val shareRepo: ShareRepo,
) {

    fun setup(rootRoute: RootRoute) {
        rootRoute.post("$BASE_PATH/register") {
            try {
                handleRegister()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Account creation failed")
            }
        }
        rootRoute.post("$BASE_PATH/share") {
            try {
                handleShare()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Share link creation failed")
            }
        }
    }

    private suspend fun RoutingContext.handleRegister() {
        val deviceId = call.request.header("X-Device-ID")
        val shareCode = call.request.queryParameters["share"]

        log(TAG) { "register($callInfo): deviceId=$deviceId, shareCode=$shareCode" }

        if (deviceId == null) {
            log(TAG, WARN) { "register($callInfo): 400 Bad request, missing header ID" }
            call.respond(HttpStatusCode.BadRequest, "X-Device-ID header is missing")
            return
        }

        val credentials = this.deviceCredentials
        log(TAG, VERBOSE) { "register($callInfo): credentials=$credentials" }

        // Check if this device is already registered
        var device = deviceRepo.getDevice(deviceId)
        if (device != null) {
            log(TAG, WARN) { "register($callInfo): Device is already known: $device" }
            call.respond(HttpStatusCode.BadRequest, "Device is already registered")
            return
        }

        // At this point there is no device registered with $deviceId yet
        var account = credentials?.let { accountRepo.getAccount(it.accountId) }
        if (account != null) log(TAG) { "register($callInfo): Found matching account: $account" }

        // Can we add this device to an existing account?
        if (shareCode != null) {
            if (account == null) {
                log(TAG, WARN) { "register($callInfo): Can't use shareId, no account provided" }
                call.respond(HttpStatusCode.BadRequest, "Unknown account")
                return
            }

            val share = shareRepo.resolveCode(shareCode)
            if (share == null) {
                log(TAG, WARN) { "register($callInfo): Could not resolve ShareCode" }
                call.respond(HttpStatusCode.Forbidden, "Invalid ShareCode")
                return
            }
            if (share.accountId != credentials?.accountId) {
                log(TAG, WARN) { "register($callInfo): Share invalid, account ID mismatch" }
                call.respond(HttpStatusCode.Forbidden, "Invalid account")
                return
            }

            log(TAG, INFO) { "register($callInfo): Share was valid and matches account, let's add the device" }
        } else {
            // No ShareCode, can we create a new account?
            if (credentials != null) {
                log(TAG, WARN) { "register($callInfo): Credentials provided but no ShareCode" }
                call.respond(HttpStatusCode.BadRequest, "Provide ShareCode if you provide credentials")
                return
            }

            log(TAG, INFO) { "register($callInfo): No ShareCode and Account does not exist, create one and add device" }
            account = accountRepo.createAccount()
        }

        device = deviceRepo.createDevice(
            accountId = account.id,
            label = call.request.headers["User-Agent"] ?: ""
        )

        val response = RegisterResponse(
            accountID = device.accountId,
            password = device.password,
        )
        call.respond(response).also {
            log(TAG, INFO) { "register($callInfo): Device registered $account - $device" }
        }
    }

    private suspend fun RoutingContext.handleShare() {
        val deviceId = call.request.header("X-Device-ID")
        log(TAG) { "share($callInfo): deviceId=$deviceId" }

        if (deviceId == null) {
            log(TAG, WARN) { "share($callInfo): 400 Bad request, missing header ID" }
            call.respond(HttpStatusCode.BadRequest, "X-Device-ID header is missing")
            return
        }

        // Check credentials

        // Get matching account

        // Generate share code for account

        // return ShareCode
    }

    companion object {
        private const val BASE_PATH = "/v1/auth"
        private val TAG = logTag("Account", "AuthRoute")
    }
}