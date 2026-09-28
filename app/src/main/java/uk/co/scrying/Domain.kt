package uk.co.scrying

import java.util.UUID

enum class OwnershipState { OBSERVED, OWNERSHIP_UNKNOWN, MINE, PERMISSION_GRANTED, PERMISSION_DENIED }
enum class AvailabilityState { UNKNOWN, AVAILABLE_RESOURCE, ASSIGNED, OFFLINE, RETIRED }
enum class TechnologyCategory { PHONE, COMPUTER, ROUTER, SWITCH, STORAGE, SENSOR, IOT, NETWORK, UNKNOWN }

data class TechnologyNode(
    val id: String = UUID.randomUUID().toString(),
    val friendlyName: String,
    val category: TechnologyCategory = TechnologyCategory.UNKNOWN,
    val ownership: OwnershipState = OwnershipState.OBSERVED,
    val availability: AvailabilityState = AvailabilityState.UNKNOWN,
    val capabilities: Set<String> = emptySet(),
    val firstSeen: Long = System.currentTimeMillis(),
    val lastSeen: Long = firstSeen,
    val observations: Int = 1,
    val rssi: Int? = null
)

data class Observation(val nodeId: String, val source: String, val time: Long, val rssi: Int?, val detail: String)
data class ProjectPlan(val goal: String, val assignments: List<String>, val gaps: List<String>, val explanation: String)

enum class CapabilityState { AVAILABLE, DISABLED, PERMISSION_REQUIRED, UNSUPPORTED, UNKNOWN }
data class DeviceCapability(val title: String, val detail: String, val state: CapabilityState)
data class DeviceProfile(val deviceName: String, val summary: String, val capabilities: List<DeviceCapability>)
enum class ContributionType { DEVICE_HEALTH, BLE_OBSERVER, WIFI_OBSERVER, MOTION_SENSOR, MAGNETIC_SENSOR, LOCATION_SENSOR, CAMERA_NODE }
data class Person(val id: String = UUID.randomUUID().toString(), val displayName: String, val note: String = "", val createdAt: Long = System.currentTimeMillis())
data class SharingAgreement(val personId: String, val contribution: ContributionType, val enabled: Boolean, val updatedAt: Long = System.currentTimeMillis())

data class InstrumentState(
    val running: Boolean = false,
    val status: String = "Instruments idle",
    val accelerationMs2: Double? = null,
    val magneticUt: Double? = null,
    val lightLux: Double? = null,
    val pressureHpa: Double? = null,
    val relativeSoundDbfs: Double? = null,
    val microphoneEnabled: Boolean = false
)

data class EnvironmentalSession(
    val id: String = UUID.randomUUID().toString(),
    val placeLabel: String,
    val startedAt: Long,
    val endedAt: Long,
    val samples: Int,
    val accelerationRms: Double?,
    val magneticUt: Double?,
    val lightLux: Double?,
    val pressureHpa: Double?,
    val relativeSoundDbfs: Double?,
    val note: String = ""
)

data class EnvironmentalFinding(val title: String, val detail: String, val level: FindingLevel)
enum class FindingLevel { BASELINE, CHANGED, CHECK, UNAVAILABLE }

object EnvironmentalAnalysis {
    /** Evidence-only local comparison. It does not identify people, diagnose hazards, or infer RF from magnetism. */
    fun analyse(session: EnvironmentalSession, prior: List<EnvironmentalSession>): List<EnvironmentalFinding> {
        val samePlace = prior.filter { it.placeLabel.equals(session.placeLabel, true) && it.id != session.id }
        if (samePlace.isEmpty()) return listOf(EnvironmentalFinding("First local baseline", "This is the first saved reading for ${session.placeLabel}. Repeat a session here to compare conditions.", FindingLevel.BASELINE)) + unavailable(session)
        val findings = mutableListOf<EnvironmentalFinding>()
        compare("Magnetic field", session.magneticUt, samePlace.mapNotNull { it.magneticUt }, "µT", findings)
        compare("Relative sound", session.relativeSoundDbfs, samePlace.mapNotNull { it.relativeSoundDbfs }, "dBFS", findings)
        compare("Light", session.lightLux, samePlace.mapNotNull { it.lightLux }, "lux", findings)
        compare("Pressure", session.pressureHpa, samePlace.mapNotNull { it.pressureHpa }, "hPa", findings)
        if (session.accelerationRms != null) findings += EnvironmentalFinding("Motion / vibration", "Session RMS ${"%.2f".format(session.accelerationRms)} m/s². This measures phone motion, not a cause or source.", FindingLevel.BASELINE)
        return findings + unavailable(session)
    }
    private fun compare(name: String, value: Double?, history: List<Double>, unit: String, target: MutableList<EnvironmentalFinding>) {
        if (value == null || history.isEmpty()) return
        val baseline = history.average(); val delta = value - baseline; val percent = if (baseline == 0.0) 0.0 else kotlin.math.abs(delta / baseline) * 100
        val level = if (percent >= 25) FindingLevel.CHANGED else FindingLevel.BASELINE
        val wording = if (level == FindingLevel.CHANGED) "changed ${"%.0f".format(percent)}% from your ${history.size}-session local baseline" else "is within 25% of your local baseline"
        target += EnvironmentalFinding(name, "${"%.1f".format(value)} $unit $wording.", level)
    }
    private fun unavailable(s: EnvironmentalSession): List<EnvironmentalFinding> = buildList {
        if (s.lightLux == null) add(EnvironmentalFinding("Light", "No light sensor reading was available on this phone.", FindingLevel.UNAVAILABLE))
        if (s.pressureHpa == null) add(EnvironmentalFinding("Pressure", "No pressure sensor reading was available on this phone.", FindingLevel.UNAVAILABLE))
        if (s.relativeSoundDbfs == null) add(EnvironmentalFinding("Relative sound", "Microphone measurement was not enabled for this session.", FindingLevel.UNAVAILABLE))
    }
}

enum class ChangeType { NEW_DEVICE, DEVICE_RETURNED, KNOWN_RESOURCE_OFFLINE, RESOURCE_AVAILABLE }
data class EnvironmentChange(val type: ChangeType, val node: TechnologyNode, val explanation: String)

object EnvironmentChangeDetector {
    /** Conservative evidence-only changes. It never asserts intent or a physical identity. */
    fun detect(nodes: List<TechnologyNode>, now: Long = System.currentTimeMillis()): List<EnvironmentChange> = nodes.mapNotNull { node ->
        when {
            node.firstSeen >= now - 24 * 60 * 60 * 1000L -> EnvironmentChange(ChangeType.NEW_DEVICE, node, "First passive observation was within the last 24 hours.")
            node.ownership in setOf(OwnershipState.MINE, OwnershipState.PERMISSION_GRANTED) && now - node.lastSeen > 24 * 60 * 60 * 1000L -> EnvironmentChange(ChangeType.KNOWN_RESOURCE_OFFLINE, node, "No observation or user update was recorded for more than 24 hours; it may simply be powered off or out of range.")
            else -> null
        }
    }
}

object ProjectCompiler {
    fun compile(goal: String, resources: List<TechnologyNode>): ProjectPlan {
        val normalized = goal.lowercase()
        val computer = resources.firstOrNull { it.category == TechnologyCategory.COMPUTER }
        val phone = resources.firstOrNull { it.category == TechnologyCategory.PHONE }
        val network = resources.firstOrNull { it.category in setOf(TechnologyCategory.ROUTER, TechnologyCategory.SWITCH, TechnologyCategory.NETWORK) }
        val assignments = mutableListOf<String>()
        val gaps = mutableListOf<String>()
        when {
            "camera" in normalized || "cctv" in normalized -> {
                if (phone != null) assignments += "${phone.friendlyName} → camera and sensor node"
                else gaps += "An authorised phone or IP camera"
                if (computer != null) assignments += "${computer.friendlyName} → recording and storage"
                else gaps += "A computer or storage node for recordings"
                if (network != null) assignments += "${network.friendlyName} → local network backbone"
                else gaps += "A local network connection"
            }
            "ai" in normalized || "llm" in normalized -> {
                if (computer != null) assignments += "${computer.friendlyName} → local inference worker"
                else gaps += "A computer with sufficient RAM (GPU optional)"
                if (phone != null) assignments += "${phone.friendlyName} → monitoring dashboard / sensor input"
                if (network != null) assignments += "${network.friendlyName} → LAN connectivity"
            }
            else -> resources.take(3).forEach { assignments += "${it.friendlyName} → evaluate as ${it.category.name.lowercase()} resource" }
        }
        if (assignments.isEmpty()) gaps += "Authorised technology in My Technology"
        return ProjectPlan(goal, assignments, gaps, "Rule-based plan: only user-authorised resources were considered. Review each assignment before deploying software.")
    }
}
