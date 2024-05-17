package eu.darken.octi.kserver.module

import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.VERBOSE
import eu.darken.octi.kserver.common.debug.logging.log
import eu.darken.octi.kserver.common.debug.logging.logTag
import eu.darken.octi.kserver.device.Device
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
@Singleton
class ModuleRepo @Inject constructor(
    private val serializer: Json,
) {

    private fun Device.getModulePath(moduleId: ModuleId): Path {
        val digest = MessageDigest.getInstance("SHA-1")
        val hashBytes = digest.digest(moduleId.toByteArray())
        val safeName = hashBytes.joinToString("") { "%02x".format(it) }
        return path.resolve("$MODULES_DIR/$safeName")
    }

    suspend fun read(caller: Device, target: Device, moduleId: ModuleId): Module.Read {
        log(TAG, VERBOSE) { "write(${caller.id}, ${target.id}, $moduleId) reading..." }
        val modulePath = target.getModulePath(moduleId)

        return target.sync.withLock {
            if (!modulePath.exists()) {
                Module.Read()
            } else {
                Module.Read(
                    modifiedAt = modulePath.resolve(BLOB_FILENAME).getLastModifiedTime().toInstant(),
                    payload = modulePath.resolve(BLOB_FILENAME).readBytes(),
                )
            }
        }.also {
            log(TAG, VERBOSE) { "write(${caller.id}, ${target.id}, $moduleId) ${it.size}B read" }
        }
    }

    suspend fun write(caller: Device, target: Device, moduleId: ModuleId, write: Module.Write) {
        log(TAG, VERBOSE) { "write(${caller.id}, ${target.id}, $moduleId) payload is ${write.size}B ..." }
        val modulePath = target.getModulePath(moduleId)
        val info = Module.Info(
            id = moduleId,
            source = caller.id,
        )
        target.sync.withLock {
            modulePath.apply {
                if (!parent.exists()) parent.createDirectory() // modules dir
                if (!exists()) createDirectory() // specific module dir
                resolve(BLOB_FILENAME).writeBytes(write.payload)
                resolve(META_FILENAME).writeText(serializer.encodeToString(info))
            }
        }
        log(TAG, VERBOSE) { "write(${caller.id}, ${target.id}, $moduleId) payload written" }
    }

    suspend fun delete(caller: Device, target: Device, moduleId: ModuleId) {
        log(TAG, VERBOSE) { "delete(${caller.id}, ${target.id}, $moduleId): Deleting module..." }
        val modulePath = target.getModulePath(moduleId)

        target.sync.withLock {
            if (!modulePath.exists()) {
                log(TAG) { "delete(${caller.id}, ${target.id}, $moduleId): Module didn't exist" }
                return
            }
            val info: Module.Info = serializer.decodeFromString(modulePath.resolve(META_FILENAME).readText())
            modulePath.deleteRecursively()
            log(TAG) { "delete(${caller.id}, ${target.id}, $moduleId): Module deleted $info" }
        }
    }

    companion object {
        private const val MODULES_DIR = "modules"
        private const val META_FILENAME = "module.json"
        private const val BLOB_FILENAME = "payload.blob"
        private val TAG = logTag("Module", "Repo")
    }
}