package uk.co.scrying

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

class ScannerRepository(private val context: Context) {
    private val _observations = MutableStateFlow<List<TechnologyNode>>(emptyList())
    val observations = _observations.asStateFlow()
    private var callback: ScanCallback? = null
    private val _status = MutableStateFlow("Not observing")
    val status = _status.asStateFlow()

    @SuppressLint("MissingPermission")
    fun scanBle() {
        try {
            val scanner: BluetoothLeScanner = BluetoothAdapter.getDefaultAdapter()?.bluetoothLeScanner ?: run { _status.value = "Bluetooth is unavailable or switched off"; return }
            callback?.let { scanner.stopScan(it) }
            callback = object : ScanCallback() {
            override fun onScanResult(type: Int, result: ScanResult) {
                val name = result.device.name ?: "Unnamed BLE device"
                // Android radio identifiers can rotate and are personal data; retain only a local hash.
                val stable = "ble:${privacySafeId(result.device.address)}"
                upsert(TechnologyNode(id = stable, friendlyName = name, rssi = result.rssi, lastSeen = System.currentTimeMillis()))
            }
            }
            scanner.startScan(callback)
            _status.value = "BLE observer running"
        } catch (_: SecurityException) { _status.value = "Bluetooth permission is required before observing" }
    }

    @SuppressLint("MissingPermission")
    fun scanWifi() {
        try {
            val wifi = context.applicationContext.getSystemService(WifiManager::class.java) ?: run { _status.value = "Wi-Fi service unavailable"; return }
            wifi.startScan()
            wifi.scanResults.forEach { result ->
            upsert(TechnologyNode(id = "wifi:${privacySafeId(result.BSSID)}", friendlyName = result.SSID.ifBlank { "Hidden Wi-Fi network" }, category = TechnologyCategory.NETWORK, rssi = result.level, lastSeen = System.currentTimeMillis()))
            }
            _status.value = "Wi-Fi observation requested; Android may delay results"
        } catch (_: SecurityException) { _status.value = "Location or nearby Wi-Fi permission is required before observing" }
    }
    fun stop() { try { BluetoothAdapter.getDefaultAdapter()?.bluetoothLeScanner?.let { s -> callback?.let(s::stopScan) } } catch (_: SecurityException) { }; _status.value = "Not observing" }
    private fun upsert(incoming: TechnologyNode) {
        val existing = _observations.value.firstOrNull { it.id == incoming.id }
        val updated = if (existing == null) incoming else existing.copy(lastSeen = incoming.lastSeen, observations = existing.observations + 1, rssi = incoming.rssi)
        _observations.value = _observations.value.filterNot { it.id == updated.id } + updated
    }
    private fun privacySafeId(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }.take(24)
}
