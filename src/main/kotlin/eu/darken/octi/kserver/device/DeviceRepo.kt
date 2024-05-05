package eu.darken.octi.kserver.device

import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.INFO
import eu.darken.octi.kserver.common.debug.logging.log
import eu.darken.octi.kserver.common.debug.logging.logTag
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepo @Inject constructor() {

    private val devices = mutableMapOf<String, Device>()
    private val mutex = Mutex()

    suspend fun createDevice(
        accountId: String,
        label: String,
    ): Device {
        return Device(
            accountId = accountId,
            label = label,
        ).also {
            devices[it.id] = it
            log(TAG, INFO) { "createDevice(): Device created $it" }
        }
    }

    suspend fun getDevice(id: String): Device? {
        return devices[id]
    }

    companion object {
        private val TAG = logTag("Device", "Repo")
    }
}