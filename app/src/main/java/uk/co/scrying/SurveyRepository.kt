package uk.co.scrying

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SurveyPoint(val latitude: Double, val longitude: Double, val accuracyMetres: Float, val time: Long)

class SurveyRepository(context: Context) {
    private val manager = context.getSystemService(LocationManager::class.java)
    private val _points = MutableStateFlow<List<SurveyPoint>>(emptyList())
    private val _status = MutableStateFlow("Survey inactive")
    val points = _points.asStateFlow(); val status = _status.asStateFlow()
    private val listener = LocationListener { location -> add(location) }
    @SuppressLint("MissingPermission") fun start() = try {
        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3_000, 3f, listener, Looper.getMainLooper())
        manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5_000, 8f, listener, Looper.getMainLooper())
        _status.value = "Active survey: recording location with reported accuracy"
    } catch (_: SecurityException) { _status.value = "Location permission is required for an active survey" }
    fun stop() { manager.removeUpdates(listener); _status.value = "Survey paused" }
    private fun add(location: Location) {
        if (location.accuracy > 500f) return
        val point = SurveyPoint(location.latitude, location.longitude, location.accuracy, location.time)
        val previous = _points.value.lastOrNull()
        if (previous != null && location.time - previous.time < 1_000 && previous.latitude == point.latitude && previous.longitude == point.longitude) return
        _points.value = (_points.value + point).takeLast(500)
    }
}
