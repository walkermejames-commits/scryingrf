package uk.co.scrying

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.concurrent.thread
import kotlin.math.log10
import kotlin.math.sqrt

/** Reads only Android sensors while a foreground session is explicitly running. */
class InstrumentRepository(context: Context) : SensorEventListener {
    private val sensors = context.getSystemService(SensorManager::class.java)
    private val _state = MutableStateFlow(InstrumentState())
    val state = _state.asStateFlow()
    private var startedAt = 0L; private var samples = 0; private var accelSquares = 0.0
    private var magneticSum = 0.0; private var magneticSamples = 0; private var lightSum = 0.0; private var lightSamples = 0; private var pressureSum = 0.0; private var pressureSamples = 0
    private var audio: AudioRecord? = null; private var audioThread: Thread? = null; @Volatile private var audioRunning = false; private var soundSum = 0.0; private var soundSamples = 0

    fun start(withMicrophone: Boolean) {
        stopSensorsOnly(); startedAt = System.currentTimeMillis(); samples = 0; accelSquares = 0.0; magneticSum = 0.0; magneticSamples = 0; lightSum = 0.0; lightSamples = 0; pressureSum = 0.0; pressureSamples = 0; soundSum = 0.0; soundSamples = 0
        listOf(Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_MAGNETIC_FIELD, Sensor.TYPE_LIGHT, Sensor.TYPE_PRESSURE).forEach { type -> sensors?.getDefaultSensor(type)?.let { sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) } }
        if (withMicrophone) startMicrophone()
        _state.value = InstrumentState(running = true, status = "Recording local environmental readings", microphoneEnabled = withMicrophone)
    }

    fun finish(place: String, note: String = ""): EnvironmentalSession? {
        if (!_state.value.running) return null
        val endedAt = System.currentTimeMillis(); stopSensorsOnly(); stopMicrophone()
        val result = EnvironmentalSession(placeLabel = place.ifBlank { "Unlabelled place" }, startedAt = startedAt, endedAt = endedAt, samples = samples, accelerationRms = if (samples == 0) null else sqrt(accelSquares / samples), magneticUt = magneticSum.takeIf { magneticSamples > 0 }?.div(magneticSamples), lightLux = lightSum.takeIf { lightSamples > 0 }?.div(lightSamples), pressureHpa = pressureSum.takeIf { pressureSamples > 0 }?.div(pressureSamples), relativeSoundDbfs = soundSum.takeIf { soundSamples > 0 }?.div(soundSamples), note = note)
        _state.value = InstrumentState(status = "Saved ${result.samples} sensor samples")
        return result
    }

    override fun onSensorChanged(event: SensorEvent) {
        val magnitude = sqrt(event.values.sumOf { (it * it).toDouble() })
        val current = _state.value
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> { samples++; accelSquares += magnitude * magnitude; _state.value = current.copy(accelerationMs2 = magnitude) }
            Sensor.TYPE_MAGNETIC_FIELD -> { magneticSamples++; magneticSum += magnitude; _state.value = current.copy(magneticUt = magnitude) }
            Sensor.TYPE_LIGHT -> { lightSamples++; lightSum += event.values[0]; _state.value = current.copy(lightLux = event.values[0].toDouble()) }
            Sensor.TYPE_PRESSURE -> { pressureSamples++; pressureSum += event.values[0]; _state.value = current.copy(pressureHpa = event.values[0].toDouble()) }
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    private fun stopSensorsOnly() { sensors?.unregisterListener(this) }

    private fun startMicrophone() {
        val size = AudioRecord.getMinBufferSize(8_000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(4_096)
        try {
            audio = AudioRecord(MediaRecorder.AudioSource.DEFAULT, 8_000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, size).also { it.startRecording() }
            audioRunning = true
            audioThread = thread(name = "scrying-audio-meter") { val buffer = ShortArray(size / 2); while (audioRunning) { val read = audio?.read(buffer, 0, buffer.size) ?: 0; if (read > 0) { val rms = sqrt(buffer.take(read).sumOf { it.toDouble() * it } / read); val dbfs = 20 * log10((rms / Short.MAX_VALUE).coerceAtLeast(0.000_001)); soundSum += dbfs; soundSamples++; _state.value = _state.value.copy(relativeSoundDbfs = dbfs) } } }
        } catch (_: SecurityException) { _state.value = _state.value.copy(status = "Microphone permission was not granted") }
    }
    private fun stopMicrophone() { audioRunning = false; try { audio?.stop(); audio?.release() } catch (_: Exception) { }; audio = null; audioThread = null }
}
