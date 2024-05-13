package eu.darken.octi.kserver.device

import eu.darken.octi.kserver.account.Account
import eu.darken.octi.kserver.account.AccountRepo
import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.*
import eu.darken.octi.kserver.common.debug.logging.asLog
import eu.darken.octi.kserver.common.debug.logging.log
import eu.darken.octi.kserver.common.debug.logging.logTag
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepo @Inject constructor(
    private val serializer: Json,
    private val accountsRepo: AccountRepo,
) {

    private val devices = mutableMapOf<DeviceId, Device>()
    private val mutex = Mutex()

    init {
        runBlocking {
            accountsRepo.getAllAccounts()
                .asSequence()
                .mapNotNull { account ->
                    try {
                        File(account.path, DEVICES_DIR).listFiles()?.toList().also {
                            log(TAG) { "Loading account with ${it?.size} devices" }
                        }
                    } catch (e: IOException) {
                        log(TAG, ERROR) { "Failed to list devices for $account" }
                        null
                    }
                }
                .flatten()
                .forEach { deviceDir ->
                    val deviceData = try {
                        serializer.decodeFromString<Device.Data>(File(deviceDir, DEVICE_FILENAME).readText())
                    } catch (e: IOException) {
                        log(TAG, ERROR) { "Failed to read $deviceDir: ${e.asLog()}" }
                        return@forEach
                    }
                    log(TAG) { "Device info loaded: $deviceData" }
                    devices[deviceData.id] = Device(
                        data = deviceData,
                        path = deviceDir,
                    )
                }
            log(TAG, INFO) { "${devices.size} devices loaded into memory" }
        }
    }

    suspend fun createDevice(
        account: Account,
        label: String,
    ): Device = mutex.withLock {
        val data = Device.Data(
            accountId = account.id,
            label = label,
        )

        val device = Device(
            data = data,
            path = File(account.path, "$DEVICES_DIR/${data.id}")
        )
        device.path.run {
            if (mkdirs()) log(TAG) { "Created dirs: $this" }
            File(this, DEVICE_FILENAME).writeText(serializer.encodeToString(data))
            log(TAG, VERBOSE) { "Device written: $this" }
        }

        devices[device.id] = device
        log(TAG, INFO) { "createDevice(): Device created $device" }

        return device
    }

    suspend fun getDevice(id: DeviceId): Device? {
        return devices[id]
    }

    suspend fun deleteDevice(id: String) = mutex.withLock {
        log(TAG, INFO) { "deleteDevice($id)" }
    }

    companion object {
        private const val DEVICES_DIR = "devices"
        private const val DEVICE_FILENAME = "device.json"
        private val TAG = logTag("Device", "Repo")
    }
}