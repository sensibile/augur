package me.sensibile.augur.rule.management

import me.sensibile.augur.rule.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals

class RuleManagementFlagEventApplierTest {
    @Test
    fun `applies flag added event`() {
        val state = draftState()
        val flag = flag("new_checkout")
        val event = FlagAdded(eventId = eventId(), draftId = state.draftId, flag = flag)

        val actual = RuleManagementEventApplier.apply(state = state, event = event)

        val updated = (actual as Outcome.Ok).value
        assertEquals(mapOf(flag.key to flag), updated.flags)
        assertEquals(RuleSetDraftStatus.Draft, updated.status)
    }

    @Test
    fun `applies flag enabled event`() {
        val flag = flag("new_checkout", enabled = false)
        val state = draftState(flags = mapOf(flag.key to flag))
        val event = FlagEnabled(eventId = eventId(), draftId = state.draftId, flagKey = flag.key)

        val actual = RuleManagementEventApplier.apply(state = state, event = event)

        val updated = (actual as Outcome.Ok).value
        assertEquals(true, updated.flags.getValue(flag.key).enabled)
        assertEquals(RuleSetDraftStatus.Draft, updated.status)
    }

    @Test
    fun `applies flag disabled event`() {
        val flag = flag("new_checkout", enabled = true)
        val state = draftState(flags = mapOf(flag.key to flag))
        val event = FlagDisabled(eventId = eventId(), draftId = state.draftId, flagKey = flag.key)

        val actual = RuleManagementEventApplier.apply(state = state, event = event)

        val updated = (actual as Outcome.Ok).value
        assertEquals(false, updated.flags.getValue(flag.key).enabled)
        assertEquals(RuleSetDraftStatus.Draft, updated.status)
    }

    @Test
    fun `rejects flag status event when flag does not exist`() {
        val state = draftState()
        val flagKey = flagKey("new_checkout")
        val event = FlagEnabled(eventId = eventId(), draftId = state.draftId, flagKey = flagKey)

        val actual = RuleManagementEventApplier.apply(state = state, event = event)

        assertEquals(Outcome.Err(RuleManagementEventApplyError.FlagNotFound(state.draftId, flagKey)), actual)
    }

    @Test
    fun `rejects event without draft state`() {
        val event =
            FlagAdded(
                eventId = eventId(),
                draftId = draftId(),
                flag = flag("new_checkout"),
            )

        val actual = RuleManagementEventApplier.apply(state = null, event = event)

        assertEquals(Outcome.Err(RuleManagementEventApplyError.DraftNotFound(event.draftId)), actual)
    }
}
