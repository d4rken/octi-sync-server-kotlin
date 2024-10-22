package eu.darken.octi.kserver.device

import eu.darken.octi.kserver.common.callInfo
import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.ERROR
import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.INFO
import eu.darken.octi.kserver.common.debug.logging.asLog
import eu.darken.octi.kserver.common.debug.logging.log
import eu.darken.octi.kserver.common.debug.logging.logTag
import eu.darken.octi.kserver.common.verifyCaller
import eu.darken.octi.kserver.module.ModuleRepo
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRoute @Inject constructor(
    private val deviceRepo: DeviceRepo,
    private val moduleRepo: ModuleRepo,
) {

    fun setup(rootRoute: Routing) {
        rootRoute.route("/v1/devices") {
            get {
                try {
                    getDevices()
                } catch (e: Exception) {
                    log(TAG, ERROR) { "getDevices() failed: ${e.asLog()}" }
                    call.respond(HttpStatusCode.InternalServerError, "Failed to list devices")
                }
            }
            delete("/{deviceId}") {
                val deviceId: DeviceId? = call.parameters["deviceId"]?.let { UUID.fromString(it) }
                if (deviceId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Missing deviceId")
                    return@delete
                }
                try {
                    deleteDevice(deviceId)
                } catch (e: Exception) {
                    log(TAG, ERROR) { "deleteDevice($deviceId) failed: ${e.asLog()}" }
                    call.respond(HttpStatusCode.InternalServerError, "Failed to delete device")
                }
            }
            post("/reset") {
                try {
                    resetDevices()
                } catch (e: Exception) {
                    log(TAG, ERROR) { "resetDevices() failed: ${e.asLog()}" }
                    call.respond(HttpStatusCode.InternalServerError, "Failed to reset devices")
                }
            }
        }
    }

    private suspend fun RoutingContext.getDevices() {
        val callerDevice = verifyCaller(TAG, deviceRepo) ?: return

        val devices = deviceRepo.getDevices(callerDevice.accountId)
        val response = DevicesResponse(
            devices = devices.map {
                DevicesResponse.Device(
                    id = it.id,
                    version = it.version,
                )
            }
        )
        call.respond(response).also { log(TAG) { "getDevices($callInfo): -> $response" } }
    }

    private suspend fun RoutingContext.deleteDevice(deviceId: DeviceId) {
        val callerDevice = verifyCaller(TAG, deviceRepo) ?: return
        val targetDevice = deviceRepo.getDevice(deviceId)
        if (targetDevice == null) {
            call.respond(HttpStatusCode.NotFound) { "Device not found $deviceId" }
            return
        }
        if (targetDevice.accountId != callerDevice.accountId) {
            call.respond(HttpStatusCode.Unauthorized) { "Device does not belong to your account" }
            return
        }

        deviceRepo.deleteDevice(deviceId)
        moduleRepo.clear(callerDevice, setOf(targetDevice))

        call.respond(HttpStatusCode.OK).also {
            log(TAG, INFO) { "delete($callInfo): Device was deleted: $deviceId" }
        }
    }

    private suspend fun RoutingContext.resetDevices() {
        val callerDevice = verifyCaller(TAG, deviceRepo) ?: return

        var targetDevices = call.receive<ResetRequest>().targets
            .map { deviceRepo.getDevice(it)!! }
            .toSet()


        if (targetDevices.any { it.accountId != callerDevice.accountId }) {
            call.respond(HttpStatusCode.Unauthorized) { "Devices do not belong to your account" }
            return
        }

        if (targetDevices.isEmpty()) {
            log(TAG) { "No explicit targets provided, targeting all devices of this account." }
            targetDevices = deviceRepo.getDevices(callerDevice.accountId).toSet()
        }

        log(TAG, INFO) { "resetDevices(${callInfo}): Resetting devices ${targetDevices.map { it.id }}" }

        moduleRepo.clear(callerDevice, targetDevices)

        call.respond(HttpStatusCode.OK).also {
            log(TAG, INFO) { "resetDevices($callInfo): Devices were reset" }
        }
    }

    companion object {
        private val TAG = logTag("Devices", "Route")
    }
}