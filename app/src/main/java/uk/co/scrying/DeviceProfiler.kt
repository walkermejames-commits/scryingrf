package uk.co.scrying

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.StatFs
import androidx.core.content.ContextCompat

/** Reads only Android-exposed, on-device facts. It does not activate cameras, sensors, or radios. */
class DeviceProfiler(private val context: Context) {
    fun profile(): DeviceProfile {
        val pm = context.packageManager
        val sensorManager = context.getSystemService(SensorManager::class.java)
        val bluetooth = context.getSystemService(BluetoothManager::class.java)?.adapter
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val camera = context.getSystemService(CameraManager::class.java)
        val stat = StatFs(context.filesDir.absolutePath)
        val free = stat.availableBytes / (1024 * 1024 * 1024)
        val total = stat.totalBytes / (1024 * 1024 * 1024)
        val active = connectivity?.let { manager -> manager.activeNetwork?.let { network -> manager.getNetworkCapabilities(network) } }
        val battery = context.getSystemService(BatteryManager::class.java)?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val caps = listOf(
            DeviceCapability("Device", "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}", CapabilityState.AVAILABLE),
            DeviceCapability("Compute", "${Runtime.getRuntime().availableProcessors()} CPU cores · ${Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown ABI"}", CapabilityState.AVAILABLE),
            DeviceCapability("Storage", "$free GB free of $total GB app-accessible storage", CapabilityState.AVAILABLE),
            DeviceCapability("Network", networkDetail(active), if (active == null) CapabilityState.DISABLED else CapabilityState.AVAILABLE),
            DeviceCapability("Bluetooth / BLE", if (bluetooth == null) "Bluetooth hardware unavailable" else if (bluetooth.isEnabled) "Enabled; passive observation can be requested" else "Hardware present but Bluetooth is off", if (bluetooth == null) CapabilityState.UNSUPPORTED else if (bluetooth.isEnabled) permissionState(Manifest.permission.BLUETOOTH_SCAN) else CapabilityState.DISABLED),
            DeviceCapability("Location", "GPS/network location can be used only after permission", permissionState(Manifest.permission.ACCESS_FINE_LOCATION)),
            sensorCapability(sensorManager, Sensor.TYPE_ACCELEROMETER, "Motion", "Accelerometer"),
            sensorCapability(sensorManager, Sensor.TYPE_GYROSCOPE, "Rotation", "Gyroscope"),
            sensorCapability(sensorManager, Sensor.TYPE_MAGNETIC_FIELD, "Magnetic field", "Magnetometer; this is not an RF spectrum sensor"),
            DeviceCapability("Camera", cameraDetail(camera), if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) CapabilityState.AVAILABLE else CapabilityState.UNSUPPORTED),
            DeviceCapability("Microphone", if (pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) "Hardware available; never activated without a selected contribution" else "No microphone feature reported", if (pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) CapabilityState.AVAILABLE else CapabilityState.UNSUPPORTED),
            DeviceCapability("Wi-Fi RTT", if (pm.hasSystemFeature(PackageManager.FEATURE_WIFI_RTT)) "Hardware reports 802.11mc RTT support" else "Not reported by this device", if (pm.hasSystemFeature(PackageManager.FEATURE_WIFI_RTT)) CapabilityState.AVAILABLE else CapabilityState.UNSUPPORTED),
            DeviceCapability("UWB", if (pm.hasSystemFeature("android.hardware.uwb")) "UWB hardware reported" else "Not reported by this device", if (pm.hasSystemFeature("android.hardware.uwb")) CapabilityState.AVAILABLE else CapabilityState.UNSUPPORTED),
            DeviceCapability("Battery", if (battery != null && battery >= 0) "$battery% current capacity reported" else "Battery capacity unavailable", if (battery != null && battery >= 0) CapabilityState.AVAILABLE else CapabilityState.UNKNOWN)
        )
        return DeviceProfile("This device", "${Build.MANUFACTURER} ${Build.MODEL}", caps)
    }

    private fun permissionState(permission: String) = if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) CapabilityState.AVAILABLE else CapabilityState.PERMISSION_REQUIRED
    private fun sensorCapability(manager: SensorManager?, type: Int, name: String, detail: String) = DeviceCapability(name, detail, if (manager?.getDefaultSensor(type) == null) CapabilityState.UNSUPPORTED else CapabilityState.AVAILABLE)
    private fun networkDetail(caps: NetworkCapabilities?) = when { caps == null -> "No active network"; caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Connected over Wi-Fi"; caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Connected over cellular"; caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Connected over Ethernet"; else -> "Connected transport not identified" }
    private fun cameraDetail(camera: CameraManager?) = try { "${camera?.cameraIdList?.size ?: 0} camera${if ((camera?.cameraIdList?.size ?: 0) == 1) "" else "s"} reported" } catch (_: Exception) { "Camera service unavailable" }
}
