package eu.darken.octi.kserver.share

import eu.darken.octi.kserver.account.AccountRepo
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
class ShareRoute @Inject constructor(
    private val accountRepo: AccountRepo,
    private val deviceRepo: DeviceRepo,
    private val shareRepo: ShareRepo,
) {

    fun setup(rootRoute: RootRoute) {
        rootRoute.post("/v1/share") {
            try {
                handleShareCreation()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Share code creation failed")
            }
        }
    }

    private suspend fun RoutingContext.handleShareCreation() {
        val deviceId = call.request.header("X-Device-ID")
        log(TAG) { "share($callInfo): deviceId=$deviceId" }

        if (deviceId == null) {
            log(TAG, WARN) { "share($callInfo): 400 Bad request, missing header ID" }
            call.respond(HttpStatusCode.BadRequest, "X-Device-ID header is missing")
            return
        }

        // Check credentials
        val device = deviceRepo.getDevice(deviceId)
        if (device == null) {
            log(TAG, WARN) { "share($callInfo): deviceId=$deviceId not found" }
            call.respond(HttpStatusCode.NotFound, "Device with id $deviceId does not exist")
            return
        }
        val creds = deviceCredentials
        if (creds == null) {
            log(TAG, WARN) { "share($callInfo): deviceId=$deviceId credentials missing" }
            call.respond(HttpStatusCode.Unauthorized, "Device credentials are missing")
            return
        }

        if (!device.isAuthorized(creds)) {
            log(TAG, WARN) { "share($callInfo): deviceId=$deviceId credentials not authorized" }
            call.respond(HttpStatusCode.Unauthorized, "Device credentials ")
            return
        }

        // Get matching account
        val account = accountRepo.getAccount(device.accountId)

        // Generate share code for account
        shareRepo.createShare()

        // return ShareCode
    }

    companion object {
        private val TAG = logTag("Account", "Share", "Route")
    }
}