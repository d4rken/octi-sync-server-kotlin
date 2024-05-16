package eu.darken.octi.kserver.module

import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.VERBOSE
import eu.darken.octi.kserver.common.debug.logging.log
import eu.darken.octi.kserver.common.debug.logging.logTag
import eu.darken.octi.kserver.device.Device
import eu.darken.octi.kserver.device.DeviceRepo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
@Singleton
class ModuleRepo @Inject constructor(
    private val serializer: Json,
    private val deviceRepo: DeviceRepo,
) {

    private val modules = ConcurrentHashMap<ModuleId, Module>()
    private val mutex = Mutex()

    init {
//        runBlocking {
//            deviceRepo.getAccounts()
//                .asSequence()
//                .mapNotNull { account ->
//                    try {
//                        Files
//                            .newDirectoryStream(account.path.resolve(DEVICES_DIR))
//                            .map { account to it }
//                            .also { log(TAG) { "Listing devices for ${account.id}" } }
//                    } catch (e: IOException) {
//                        log(TAG, ERROR) { "Failed to list devices for $account" }
//                        null
//                    }
//                }
//                .flatten()
//                .forEach { (account, deviceDir) ->
//                    val deviceData = try {
//                        serializer.decodeFromString<Device.Data>(deviceDir.resolve(DEVICE_FILENAME).readText())
//                    } catch (e: IOException) {
//                        log(TAG, ERROR) { "Failed to read $deviceDir: ${e.asLog()}" }
//                        return@forEach
//                    }
//                    log(TAG) { "Device info loaded: $deviceData" }
//                    devices[deviceData.id] = Device(
//                        data = deviceData,
//                        path = deviceDir,
//                        accountId = account.id,
//                    )
//                }
//            log(TAG, INFO) { "${devices.size} devices loaded into memory" }
//        }
    }

    suspend fun read(caller: Device, target: Device, moduleId: ModuleId): Module.Read {
        log(TAG, VERBOSE) { "read($caller, $target, $moduleId)..." }
        val module = modules[moduleId] ?: return Module.Read()
        module.requireSameAccount(caller)

        return module.run {
            sync.withLock {
                path.run {
                    if (!exists()) {
                        Module.Read()
                    } else {
                        Module.Read(
                            modifiedAt = path.resolve(BLOB_FILENAME).getLastModifiedTime().toInstant(),
                            payload = path.resolve(BLOB_FILENAME).readBytes(),
                        )
                    }
                }
            }
        }
    }

    suspend fun write(device: Device, moduleId: ModuleId, payload: ByteArray) {
        log(TAG, VERBOSE) { "write($device,$moduleId) payload is ${payload.size}B ..." }
        val existing = modules.getOrPut(moduleId) {
            Module(
                accountId = device.accountId,
                deviceId = device.id,
                path = device.path.resolve("$MODULES_DIR/$moduleId"),
                meta = Module.Meta(moduleId),
            )
        }
        existing.apply {
            sync.withLock {
                path.apply {
                    if (!parent.exists()) parent.createDirectory()
                    if (!exists()) createDirectory()
                    resolve(BLOB_FILENAME).writeBytes(payload)
                    resolve(META_FILENAME).writeText(serializer.encodeToString(meta))
                }
            }
        }
    }

    suspend fun delete(device: Device, moduleId: ModuleId) {
        log(TAG, VERBOSE) { "delete(${device.id},$moduleId): Deleting module..." }
        val module = modules[moduleId] ?: throw IllegalArgumentException("$moduleId not found")
        module.requireSameAccount(device)
        module.sync.withLock {
            modules.remove(moduleId)
            module.path.deleteRecursively()
            log(TAG) { "delete(${device.id},$moduleId): Module deleted $module" }
        }
    }

    companion object {
        const val MODULES_DIR = "modules"
        private const val META_FILENAME = "module.json"
        private const val BLOB_FILENAME = "payload.blob"
        private val TAG = logTag("Module", "Repo")
    }
}