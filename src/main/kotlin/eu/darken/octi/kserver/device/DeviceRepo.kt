package eu.darken.octi.kserver.device

import eu.darken.octi.kserver.App
import eu.darken.octi.kserver.account.Account
import eu.darken.octi.kserver.account.AccountId
import eu.darken.octi.kserver.account.AccountRepo
import eu.darken.octi.kserver.common.AppScope
import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.*
import eu.darken.octi.kserver.common.debug.logging.asLog
import eu.darken.octi.kserver.common.debug.logging.log
import eu.darken.octi.kserver.common.debug.logging.logTag
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
@Singleton
class DeviceRepo @Inject constructor(
    appScope: AppScope,
    private val config: App.Config,
    private val serializer: Json,
    private val accountsRepo: AccountRepo,
) {

    private val devices = ConcurrentHashMap<DeviceId, Device>()
    private val mutex = Mutex()

    init {
        runBlocking {
            accountsRepo.getAccounts()
                .asSequence()
                .mapNotNull { account ->
                    try {
                        account.path.resolve(DEVICES_DIR)
                            .listDirectoryEntries()
                            .map { account to it }
                            .also { log(TAG, VERBOSE) { "Listing ${it.size} device(s) for account ${account.id}" } }
                    } catch (e: IOException) {
                        log(TAG, ERROR) { "Failed to list devices for $account" }
                        null
                    }
                }
                .flatten()
                .forEach { (account, deviceDir) ->
                    log(TAG, VERBOSE) { "Reading $deviceDir" }
                    val deviceData = try {
                        serializer.decodeFromString<Device.Data>(deviceDir.resolve(DEVICE_FILENAME).readText())
                    } catch (e: IOException) {
                        log(TAG, ERROR) { "Failed to read $deviceDir: ${e.asLog()}" }
                        return@forEach
                    }
                    log(TAG) { "Device info loaded: $deviceData" }
                    devices[deviceData.id] = Device(
                        data = deviceData,
                        path = deviceDir,
                        accountId = account.id,
                    )
                }
            log(TAG, INFO) { "${devices.size} devices loaded into memory" }
        }
        appScope.launch(Dispatchers.IO) {
            delay(config.deviceGCInterval.toMillis() / 10)
            while (currentCoroutineContext().isActive) {
                val now = Instant.now()
                log(TAG) { "Checking for stale devices..." }
                devices.forEach { (id, device) ->
                    if (Duration.between(device.lastSeen, now) < config.deviceExpiration) return@forEach
                    log(TAG, WARN) { "Deleting stale device $id" }
                    deleteDevice(id)
                }
                delay(config.deviceGCInterval.toMillis())
            }
        }
    }

    fun allDevices(): Collection<Device> = devices.values.toList()

    private fun Device.writeDevice() {
        path.resolve(DEVICE_FILENAME).writeText(serializer.encodeToString(data))
    }

    suspend fun createDevice(
        account: Account,
        deviceId: DeviceId,
        version: String?,
    ): Device {
        val data = Device.Data(
            id = deviceId,
            version = version,
        )
        val device = Device(
            data = data,
            accountId = account.id,
            path = account.path.resolve("$DEVICES_DIR/${data.id}")
        )
        mutex.withLock {
            if (devices[device.id] != null) throw IllegalStateException("Device already exists: ${device.id}")

            device.path.run {
                if (!parent.exists()) {
                    parent.createDirectory()
                    log(TAG) { "Created parent dir for $this" }
                }
                if (!exists()) {
                    createDirectory()
                    log(TAG) { "Created dir for $this" }
                }
            }
            device.writeDevice()
            log(TAG, VERBOSE) { "Device written: $this" }
            devices[device.id] = device
        }
        log(TAG) { "createDevice(): Device created $device" }
        return device
    }

    suspend fun getDevice(deviceId: DeviceId): Device? {
        return devices[deviceId]
    }

    suspend fun getDevices(accountId: AccountId): Collection<Device> {
        val accountDevices = mutableSetOf<Device>()
        devices.forEach {
            if (it.value.accountId == accountId) {
                accountDevices.add(it.value)
            }
        }
        return accountDevices
    }

    suspend fun deleteDevice(deviceId: DeviceId) {
        log(TAG, VERBOSE) { "deleteDevice($deviceId)..." }
        val toDelete = mutex.withLock {
            devices.remove(deviceId) ?: throw IllegalArgumentException("$deviceId not found")
        }
        toDelete.sync.withLock {
            toDelete.path.deleteRecursively()
            log(TAG) { "deleteDevice($deviceId): Device deleted: $toDelete" }
        }
    }

    suspend fun deleteDevices(accountId: AccountId) {
        log(TAG, VERBOSE) { "deleteDevices($accountId)..." }
        val toDelete = mutex.withLock {
            devices
                .filter { it.value.accountId == accountId }
                .map { devices.remove(it.key)!! }
        }
        log(TAG) { "deleteDevices($accountId): Deleting ${toDelete.size} devices" }
        toDelete.forEach { device ->
            device.sync.withLock {
                device.path.deleteRecursively()
                log(TAG) { "deleteDevices($accountId): Device deleted: $device" }
            }
        }
    }

    suspend fun updateDevice(id: DeviceId, action: (Device.Data) -> Device.Data) {
        log(TAG, VERBOSE) { "updateDevice($id)..." }
        val device = devices.values.find { it.id == id } ?: return
        device.sync.withLock {
            val newDevice = device.copy(data = action(device.data))
            newDevice.writeDevice()
            devices[id] = newDevice
        }
    }

    companion object {
        const val DEVICES_DIR = "devices"
        private const val DEVICE_FILENAME = "device.json"
        private val TAG = logTag("Device", "Repo")
    }
}