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
