package me.sensibile.augur.rule.api.management

import kotlin.test.Test
import kotlin.test.assertEquals

class RuleManagementCommandEventTest {
    @Test
    fun `create draft command carries user intent`() {
        val draftId = draftId()
        val ruleSetVersion = version(1)

        val command = CreateRuleSetDraft(draftId = draftId, ruleSetVersion = ruleSetVersion)

        assertEquals(draftId, command.draftId)
        assertEquals(ruleSetVersion, command.ruleSetVersion)
    }

    @Test
    fun `flag added event carries occurred fact`() {
        val eventId = eventId()
        val draftId = draftId()
        val flag = flag("new_checkout")

        val event = FlagAdded(eventId = eventId, draftId = draftId, flag = flag)

        assertEquals(eventId, event.eventId)
        assertEquals(draftId, event.draftId)
        assertEquals(flag, event.flag)
    }
}
