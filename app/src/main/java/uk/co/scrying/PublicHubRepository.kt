package uk.co.scrying

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import kotlin.math.floor

enum class PublicReportCategory(val wireValue: String, val label: String, val contribution: ContributionType) {
    BLE_PATTERN("BLE_PATTERN", "BLE observer summary", ContributionType.BLE_OBSERVER),
    WIFI_POSTURE("WIFI_POSTURE", "Wi-Fi observer summary", ContributionType.WIFI_OBSERVER)
}

/**
 * Sends only an explicit aggregate report. Raw coordinates, SSIDs, BSSIDs, BLE addresses,
 * names, audio, and device identifiers never leave this class.
 */
class PublicHubRepository(context: Context) {
    private val preferences = context.getSharedPreferences("public_hub", Context.MODE_PRIVATE)
    private val seed = preferences.getString("rotating_seed", null)
        ?: UUID.randomUUID().toString().also { preferences.edit().putString("rotating_seed", it).apply() }
    private val _enabled = MutableStateFlow(preferences.getBoolean("enabled", false))
    private val _status = MutableStateFlow("Public sharing is off")
    private val _insights = MutableStateFlow("No community context requested")
    val enabled = _enabled.asStateFlow()
    val status = _status.asStateFlow()
    val insights = _insights.asStateFlow()

    fun setEnabled(value: Boolean) {
        preferences.edit().putBoolean("enabled", value).apply()
        _enabled.value = value
        _status.value = if (value) "Ready: nothing is sent until you tap Share" else "Public sharing is off"
    }

    suspend fun submit(category: PublicReportCategory, point: SurveyPoint) = withContext(Dispatchers.IO) {
        if (!_enabled.value) { _status.value = "Enable public sharing before sending a report"; return@withContext }
        _status.value = "Sending privacy-filtered report…"
        try {
            val cell = coarseCell(point.latitude, point.longitude)
            val bucket = System.currentTimeMillis() / (6 * 60 * 60 * 1000L)
            val body = JSONObject().apply {
                put("schemaVersion", 1); put("category", category.wireValue); put("coarseCell", cell); put("timeBucket", bucket)
                put("rotatingToken", sha256("$seed|$cell|${category.wireValue}|$bucket"))
            }.toString()
            val connection = (URL(BASE_URL + "/v1/aggregate").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; connectTimeout = 10_000; readTimeout = 10_000
                setRequestProperty("Content-Type", "application/json"); doOutput = true
            }
            connection.outputStream.bufferedWriter().use { it.write(body) }
            val responseCode = connection.responseCode
            val accepted = responseCode in 200..299
            connection.disconnect()
            _status.value = if (accepted) "Shared one aggregate report for this six-hour period" else "The public hub rejected the report ($responseCode)"
        } catch (_: Exception) { _status.value = "Could not reach the public hub. Nothing was queued." }
    }

    suspend fun loadInsights(point: SurveyPoint) = withContext(Dispatchers.IO) {
        _insights.value = "Checking public aggregate context…"
        try {
            val connection = (URL(BASE_URL + "/v1/insights?cell=" + coarseCell(point.latitude, point.longitude)).openConnection() as HttpURLConnection).apply { connectTimeout = 10_000; readTimeout = 10_000 }
            val code = connection.responseCode
            val text = if (code in 200..299) connection.inputStream.bufferedReader().use { it.readText() } else ""
            connection.disconnect()
            val entries = if (text.isNotBlank()) JSONObject(text).optJSONArray("insights") else null
            _insights.value = if (code !in 200..299) "Public context is unavailable right now" else if (entries == null || entries.length() == 0) "No public aggregate has reached the five-contributor threshold here" else "${entries.length()} privacy-thresholded community insight(s) available"
        } catch (_: Exception) { _insights.value = "Could not load public context" }
    }

    private fun coarseCell(latitude: Double, longitude: Double): String = "${axis("n", "s", latitude)}_${axis("e", "w", longitude)}"
    private fun axis(positive: String, negative: String, value: Double): String = (if (value >= 0) positive else negative) + floor(kotlin.math.abs(value) * 10).toInt().toString().padStart(3, '0')
    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    private companion object { const val BASE_URL = "https://scrying-public-hub.walkermejames.workers.dev" }
}
