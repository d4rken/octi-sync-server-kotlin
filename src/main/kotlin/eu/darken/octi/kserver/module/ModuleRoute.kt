package eu.darken.octi.kserver.module

import eu.darken.octi.kserver.common.callInfo
import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.ERROR
import eu.darken.octi.kserver.common.debug.logging.asLog
import eu.darken.octi.kserver.common.debug.logging.log
import eu.darken.octi.kserver.common.debug.logging.logTag
import eu.darken.octi.kserver.common.verifyAuth
import eu.darken.octi.kserver.device.Device
import eu.darken.octi.kserver.device.DeviceRepo
import io.ktor.http.*
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
        val moduleId = call.parameters["moduleId"]?.let { UUID.fromString(it) }
        if (moduleId == null) call.respond(HttpStatusCode.BadRequest, "Missing moduleId")
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

    fun setup(rootRoute: RootRoute) {
        rootRoute.route("/v1/modules") {
            get("/{moduleId}") { catchError { readModule() } }
            post("/{moduleId}") { catchError { writeModule() } }
            delete("/{moduleId}") { catchError { deleteModule() } }
        }
    }

    private suspend fun RoutingContext.readModule() {
        val moduleId = requireModuleId() ?: return
        val callerDevice = verifyAuth(TAG, deviceRepo) ?: return

        val targetDevice: Device? = call.request.queryParameters["device-id"]
            ?.let { UUID.fromString(it) }
            ?.let { id ->
                val target = deviceRepo.getDevice(id)
                if (target == null) {
                    call.respond(HttpStatusCode.NotFound, "Target device was not found")
                    return
                }
                if (callerDevice.accountId != target.accountId) {
                    call.respond(HttpStatusCode.Unauthorized, "Devices don't share the same account")
                    return
                }
                target
            }

        val read = moduleRepo.read(callerDevice, targetDevice ?: callerDevice, moduleId)

        if (read.modifiedAt == null) {
            call.respond(HttpStatusCode.NoContent)
        } else {
            call.response.header("X-Modified-At", read.modifiedAt)
            call.respondBytes(
                read.payload,
                contentType = ContentType.Application.OctetStream
            )
        }.also {
            log(TAG) { "readModule($callInfo): $moduleId was read (${read.size})" }
        }
    }

    private suspend fun RoutingContext.writeModule() {
        val moduleId = requireModuleId() ?: return
        val callerDevice = verifyAuth(TAG, deviceRepo) ?: return
        val payload = call.receive<ByteArray>()
        moduleRepo.write(callerDevice, moduleId, payload)
        call.respond(HttpStatusCode.OK).also {
            log(TAG) { "writeModule($callInfo): $moduleId was written (${payload.size})" }
        }
    }

    private suspend fun RoutingContext.deleteModule() {
        val moduleId = requireModuleId() ?: return
        val callerDevice = verifyAuth(TAG, deviceRepo) ?: return
        val payload = call.receive<ByteArray>()
        moduleRepo.delete(callerDevice, moduleId)
        call.respond(HttpStatusCode.OK).also {
            log(TAG) { "deleteModule($callInfo): $moduleId was written (${payload.size})" }
        }
    }

    companion object {
        private val TAG = logTag("Module", "Route")
    }
}