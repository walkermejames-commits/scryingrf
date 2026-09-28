package uk.co.scrying

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

sealed class UpdateStatus {
    data object Idle : UpdateStatus()
    data object Checking : UpdateStatus()
    data object Current : UpdateStatus()
    data class Available(val version: String, val downloadUrl: String) : UpdateStatus()
    data class Failed(val message: String) : UpdateStatus()
}

/** Checks public release metadata only after a user action. It never downloads or installs an APK. */
class UpdateRepository {
    private val _status = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val status = _status.asStateFlow()
    fun clear() { _status.value = UpdateStatus.Idle }

    suspend fun check() = withContext(Dispatchers.IO) {
        _status.value = UpdateStatus.Checking
        try {
            val connection = (URL("https://api.github.com/repos/walkermejames-commits/scryingrf/releases/latest").openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000; readTimeout = 10_000; setRequestProperty("Accept", "application/vnd.github+json"); setRequestProperty("User-Agent", "Scrying-Android")
            }
            val responseCode = connection.responseCode
            val body = if (responseCode in 200..299) connection.inputStream.bufferedReader().use { it.readText() } else ""
            connection.disconnect()
            if (responseCode !in 200..299) { _status.value = UpdateStatus.Failed("Could not check releases ($responseCode)"); return@withContext }
            val release = JSONObject(body)
            val tag = release.optString("tag_name").removePrefix("v")
            val asset = release.optJSONArray("assets")?.let { assets -> (0 until assets.length()).map { assets.getJSONObject(it) }.firstOrNull { it.optString("name").endsWith(".apk") } }
            val url = asset?.optString("browser_download_url").orEmpty()
            val newer = isNewer(tag, BuildConfig.VERSION_NAME)
            _status.value = if (newer && url.isNotBlank()) UpdateStatus.Available(tag, url) else UpdateStatus.Current
        } catch (_: Exception) { _status.value = UpdateStatus.Failed("Could not reach GitHub. Nothing was downloaded.") }
    }

    private fun isNewer(remote: String, local: String): Boolean {
        val remoteParts = remote.split('.').map { it.toIntOrNull() ?: 0 }
        val localParts = local.split('.').map { it.toIntOrNull() ?: 0 }
        return (0 until maxOf(remoteParts.size, localParts.size)).firstOrNull { index -> (remoteParts.getOrElse(index) { 0 } != localParts.getOrElse(index) { 0 }) }?.let { index -> remoteParts.getOrElse(index) { 0 } > localParts.getOrElse(index) { 0 } } ?: false
    }
}
