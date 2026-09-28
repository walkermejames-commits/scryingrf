package uk.co.scrying

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PermissionRequest(val permissions: List<String>, val openSettings: Boolean = false)
data class ScryingUiState(val profile: DeviceProfile, val nodes: List<TechnologyNode> = emptyList(), val observed: List<TechnologyNode> = emptyList(), val people: List<Person> = emptyList(), val agreements: List<SharingAgreement> = emptyList(), val surveyPoints: List<SurveyPoint> = emptyList(), val surveyStatus: String = "Survey inactive", val scanStatus: String = "Not observing", val bleActive: Boolean = false, val wifiActive: Boolean = false, val instrument: InstrumentState = InstrumentState(), val sessions: List<EnvironmentalSession> = emptyList(), val firstRun: Boolean = true)

class ScryingViewModel(application: Application) : AndroidViewModel(application) {
    private val scanner = ScannerRepository(application); private val store = LocalTechnologyStore(application); private val survey = SurveyRepository(application); private val instruments = InstrumentRepository(application)
    val hub = PublicHubRepository(application)
    private val updater = UpdateRepository()
    val updateStatus = updater.status
    private val prefs = application.getSharedPreferences("scrying_ui", Application.MODE_PRIVATE)
    private val ble = MutableStateFlow(false); private val wifi = MutableStateFlow(false); private val profile = MutableStateFlow(DeviceProfiler(application).profile()); private val firstRun = MutableStateFlow(!prefs.getBoolean("onboarding_complete", false)); private var pendingMicSession = false
    private val requests = MutableSharedFlow<PermissionRequest>(); val permissionRequests = requests.asSharedFlow()
    private val shares = MutableSharedFlow<String>(); val shareRequests = shares.asSharedFlow()
    val state: StateFlow<ScryingUiState> = combine(store.nodes, scanner.observations, store.people, store.agreements, survey.points, survey.status, scanner.status, ble, wifi, instruments.state, store.sessions, profile, firstRun) { values ->
        @Suppress("UNCHECKED_CAST") ScryingUiState(values[11] as DeviceProfile, values[0] as List<TechnologyNode>, values[1] as List<TechnologyNode>, values[2] as List<Person>, values[3] as List<SharingAgreement>, values[4] as List<SurveyPoint>, values[5] as String, values[6] as String, values[7] as Boolean, values[8] as Boolean, values[9] as InstrumentState, values[10] as List<EnvironmentalSession>, values[12] as Boolean)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScryingUiState(profile.value, firstRun = firstRun.value))
    init { viewModelScope.launch { scanner.observations.collect { observations -> observations.forEach { store.save(it) } } } }
    fun refreshProfile() { profile.value = DeviceProfiler(getApplication()).profile() }
    fun completeOnboarding() { prefs.edit().putBoolean("onboarding_complete", true).apply(); firstRun.value = false }
    fun requestBleScan() { if (has(Manifest.permission.BLUETOOTH_SCAN)) { scanner.scanBle(); ble.value = true } else request(listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)) }
    fun requestWifiScan() { if (has(Manifest.permission.NEARBY_WIFI_DEVICES) && has(Manifest.permission.ACCESS_FINE_LOCATION)) { scanner.scanWifi(); wifi.value = true } else request(listOf(Manifest.permission.NEARBY_WIFI_DEVICES, Manifest.permission.ACCESS_FINE_LOCATION)) }
    fun stopScan() { scanner.stop(); ble.value = false; wifi.value = false }
    fun startSurvey() { if (has(Manifest.permission.ACCESS_FINE_LOCATION)) survey.start() else request(listOf(Manifest.permission.ACCESS_FINE_LOCATION)) }
    fun stopSurvey() = survey.stop()
    fun startInstrumentSession(withMicrophone: Boolean) { if (withMicrophone && !has(Manifest.permission.RECORD_AUDIO)) { pendingMicSession = true; request(listOf(Manifest.permission.RECORD_AUDIO)) } else instruments.start(withMicrophone) }
    fun finishInstrumentSession(place: String, note: String = "") = viewModelScope.launch { instruments.finish(place, note)?.let { store.save(it) } }
    fun shareSession(session: EnvironmentalSession) = viewModelScope.launch { shares.emit(buildString { append("Scrying environmental reading\n"); append("Place: ${session.placeLabel}\n"); append("Samples: ${session.samples}\n"); session.magneticUt?.let { append("Magnetic field: ${"%.1f".format(it)} µT\n") }; session.lightLux?.let { append("Light: ${"%.0f".format(it)} lux\n") }; session.pressureHpa?.let { append("Pressure: ${"%.1f".format(it)} hPa\n") }; session.relativeSoundDbfs?.let { append("Relative sound: ${"%.1f".format(it)} dBFS (not calibrated)\n") }; append("Generated locally by Scrying. This summary contains no radio identifiers or precise location.") }) }
    fun markMine(node: TechnologyNode) = viewModelScope.launch { store.save(node.copy(ownership = OwnershipState.MINE, availability = AvailabilityState.AVAILABLE_RESOURCE)) }
    fun addAuthorisedDevice(name: String, category: TechnologyCategory) = viewModelScope.launch { store.save(TechnologyNode(friendlyName = name.trim(), category = category, ownership = OwnershipState.MINE, availability = AvailabilityState.AVAILABLE_RESOURCE)) }
    fun savePerson(person: Person) = viewModelScope.launch { store.save(person) }
    fun saveAgreement(agreement: SharingAgreement) = viewModelScope.launch { store.save(agreement) }
    fun deletePerson(person: Person) = viewModelScope.launch { store.delete(person) }
    fun createPlan(goal: String, resources: List<TechnologyNode>) = ProjectCompiler.compile(goal, resources)
    fun onPermissionsResult(result: Map<String, Boolean>, permanentlyDenied: Boolean = false) { refreshProfile(); if (permanentlyDenied) { viewModelScope.launch { requests.emit(PermissionRequest(emptyList(), openSettings = true)) }; return }; if (result[Manifest.permission.RECORD_AUDIO] == true && pendingMicSession) { pendingMicSession = false; instruments.start(true) }; if (result[Manifest.permission.BLUETOOTH_SCAN] == true) requestBleScan(); if (result[Manifest.permission.NEARBY_WIFI_DEVICES] == true || result[Manifest.permission.ACCESS_FINE_LOCATION] == true) requestWifiScan() }
    fun stopAll() { scanner.stop(); survey.stop() }
    fun checkForUpdates() = viewModelScope.launch { updater.check() }
    fun clearUpdateStatus() = updater.clear()
    private fun request(permissions: List<String>) = viewModelScope.launch { requests.emit(PermissionRequest(permissions)) }
    private fun has(permission: String) = ContextCompat.checkSelfPermission(getApplication(), permission) == PackageManager.PERMISSION_GRANTED
    companion object { fun factory(application: Application) = ViewModelProvider.AndroidViewModelFactory.getInstance(application) }
}
