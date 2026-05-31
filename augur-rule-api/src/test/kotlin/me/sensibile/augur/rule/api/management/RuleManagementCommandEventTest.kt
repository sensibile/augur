package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.Flag
import me.sensibile.augur.rule.FlagKey
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.RuleSetVersion
import me.sensibile.augur.rule.RuleValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
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

    private fun draftId(): RuleSetDraftId =
        when (val draftId = RuleSetDraftId.of(Uuid.parse("018ff7c1-9354-7b02-b021-76d2791d6a21"))) {
            is Outcome.Err -> error("Invalid test draft id: ${draftId.error}")
            is Outcome.Ok -> draftId.value
        }

    private fun eventId(): RuleManagementEventId =
        when (val eventId = RuleManagementEventId.of(Uuid.parse("018ff7c1-9354-7b02-b021-76d2791d6a22"))) {
            is Outcome.Err -> error("Invalid test event id: ${eventId.error}")
            is Outcome.Ok -> eventId.value
        }

    private fun version(value: Long): RuleSetVersion =
        when (val version = RuleSetVersion.of(value)) {
            is Outcome.Err -> error("Invalid test version: ${version.error}")
            is Outcome.Ok -> version.value
        }

    private fun flag(key: String): Flag =
        Flag(
            key = flagKey(key),
            enabled = true,
            defaultValue = RuleValue.boolean(false),
            rules = emptyList(),
        )

    private fun flagKey(value: String): FlagKey =
        when (val key = FlagKey.of(value)) {
            is Outcome.Err -> error("Invalid test flag key: ${key.error}")
            is Outcome.Ok -> key.value
        }
}
