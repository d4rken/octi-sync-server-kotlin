package eu.darken.octi.kserver.module

import eu.darken.octi.kserver.common.callInfo
import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.ERROR
import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.WARN
import eu.darken.octi.kserver.common.debug.logging.asLog
import eu.darken.octi.kserver.common.debug.logging.log
import eu.darken.octi.kserver.common.debug.logging.logTag
import eu.darken.octi.kserver.common.verifyCaller
import eu.darken.octi.kserver.device.Device
import eu.darken.octi.kserver.device.DeviceId
import eu.darken.octi.kserver.device.DeviceRepo
import io.ktor.http.*
import io.ktor.server.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModuleRoute @Inject constructor(
    private val deviceRepo: DeviceRepo,
    private val moduleRepo: ModuleRepo,
) {

    private suspend fun RoutingContext.requireModuleId(): ModuleId? {
        val moduleId = call.parameters["moduleId"]
        if (moduleId == null) {
            call.respond(HttpStatusCode.BadRequest, "Missing moduleId")
            return null
        }
        if (moduleId.length > 1024) {
            call.respond(HttpStatusCode.BadRequest, "Invalid moduleId")
            return null
        }
        if (!MODULE_ID_REGEX.matches(moduleId)) {
            call.respond(HttpStatusCode.BadRequest, "Invalid moduleId")
            return null
        }
        return moduleId
    }

    private suspend fun RoutingContext.catchError(action: suspend RoutingContext.() -> Unit) {
        try {
            action()
        } catch (e: Exception) {
            log(TAG, ERROR) { "$call ${e.asLog()}" }
            call.respond(HttpStatusCode.InternalServerError, "Request failed")
        }
    }

    fun setup(rootRoute: Routing) {
        rootRoute.route("/v1/module") {
            get("/{moduleId}") { catchError { readModule() } }
            post("/{moduleId}") { catchError { writeModule() } }
            delete("/{moduleId}") { catchError { deleteModule() } }
        }
    }

    private suspend fun RoutingContext.verifyTarget(callerDevice: Device): Device? {
        val targetDeviceId: DeviceId? = call.request.queryParameters["device-id"]?.let { UUID.fromString(it) }
        if (targetDeviceId == null) {
            log(TAG, WARN) { "Caller did not supply target device: $callerDevice" }
            call.respond(HttpStatusCode.BadRequest, "Target device id not supplied")
            return null
        }
        val target = deviceRepo.getDevice(targetDeviceId)
        if (target == null) {
            log(TAG, WARN) { "Target device was not found for $targetDeviceId" }
            call.respond(HttpStatusCode.NotFound, "Target device not found")
            return null
        }

        if (callerDevice.accountId != target.accountId) {
            log(TAG, ERROR) { "Devices don't share the same account: $callerDevice and $target" }
            call.respond(HttpStatusCode.Unauthorized, "Devices don't share the same account")
            return null
        }

        return target
    }

    private suspend fun RoutingContext.readModule() {
        val moduleId = requireModuleId() ?: return
        val callerDevice = verifyCaller(TAG, deviceRepo) ?: return
        val targetDevice = verifyTarget(callerDevice) ?: return

        val read = moduleRepo.read(callerDevice, targetDevice, moduleId)

        if (read.modifiedAt == null) {
            call.respond(HttpStatusCode.NoContent)
        } else {
            call.response.header("X-Modified-At", read.modifiedAt.toHttpDateString())
            call.respondBytes(
                read.payload,
                contentType = ContentType.Application.OctetStream
            )
        }.also {
            log(TAG) { "readModule($callInfo): ${read.size}B was read from $moduleId" }
        }
    }

    private suspend fun RoutingContext.writeModule() {
        val moduleId = requireModuleId() ?: return
        val callerDevice = verifyCaller(TAG, deviceRepo) ?: return
        val targetDevice = verifyTarget(callerDevice) ?: return

        val write = Module.Write(
            payload = call.receive<ByteArray>()
        )

        moduleRepo.write(callerDevice, targetDevice, moduleId, write)
        call.respond(HttpStatusCode.OK).also {
            log(TAG) { "writeModule($callInfo): ${write.size}B was written to $moduleId" }
        }
    }

    private suspend fun RoutingContext.deleteModule() {
        val moduleId = requireModuleId() ?: return
        val callerDevice = verifyCaller(TAG, deviceRepo) ?: return
        val targetDevice = verifyTarget(callerDevice) ?: return

        moduleRepo.delete(callerDevice, targetDevice, moduleId)

        call.respond(HttpStatusCode.OK).also {
            log(TAG) { "deleteModule($callInfo): $moduleId was deleted" }
        }
    }

    companion object {
        private val MODULE_ID_REGEX = "^[a-z]+(\\.[a-z0-9_]+)*$".toRegex()
        private val TAG = logTag("Module", "Route")
    }
}