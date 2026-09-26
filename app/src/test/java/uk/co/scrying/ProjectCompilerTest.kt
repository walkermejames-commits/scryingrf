package uk.co.scrying

import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectCompilerTest {
    @Test fun cameraPlanAssignsPhoneAndComputer() {
        val resources = listOf(TechnologyNode(friendlyName = "Phone", category = TechnologyCategory.PHONE, ownership = OwnershipState.MINE), TechnologyNode(friendlyName = "Desktop", category = TechnologyCategory.COMPUTER, ownership = OwnershipState.MINE))
        val plan = ProjectCompiler.compile("home CCTV", resources)
        assertTrue(plan.assignments.any { it.contains("Phone") })
        assertTrue(plan.assignments.any { it.contains("Desktop") })
    }
    @Test fun observedItemsAreNotCompilerResources() { assertTrue(ProjectCompiler.compile("local AI", emptyList()).gaps.isNotEmpty()) }
}
