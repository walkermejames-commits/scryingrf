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

    @SuppressLint("MissingPermission")
    fun scanBle() {
        val scanner: BluetoothLeScanner = BluetoothAdapter.getDefaultAdapter()?.bluetoothLeScanner ?: return
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
    }

    @SuppressLint("MissingPermission")
    fun scanWifi() {
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java) ?: return
        wifi.startScan()
        wifi.scanResults.forEach { result ->
            upsert(TechnologyNode(id = "wifi:${privacySafeId(result.BSSID)}", friendlyName = result.SSID.ifBlank { "Hidden Wi-Fi network" }, category = TechnologyCategory.NETWORK, rssi = result.level, lastSeen = System.currentTimeMillis()))
        }
    }
    fun stop() { BluetoothAdapter.getDefaultAdapter()?.bluetoothLeScanner?.let { s -> callback?.let(s::stopScan) } }
    private fun upsert(incoming: TechnologyNode) {
        val existing = _observations.value.firstOrNull { it.id == incoming.id }
        val updated = if (existing == null) incoming else existing.copy(lastSeen = incoming.lastSeen, observations = existing.observations + 1, rssi = incoming.rssi)
        _observations.value = _observations.value.filterNot { it.id == updated.id } + updated
    }
    private fun privacySafeId(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }.take(24)
}
