package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals

class RuleManagementDraftEventApplierTest {
    @Test
    fun `applies draft created event`() {
        val event =
            RuleSetDraftCreated(
                eventId = eventId(),
                draftId = draftId(),
                ruleSetVersion = version(1),
            )

        val actual = RuleManagementEventApplier.apply(state = null, event = event)

        val state = (actual as Outcome.Ok).value
        assertEquals(event.draftId, state.draftId)
        assertEquals(event.ruleSetVersion, state.ruleSetVersion)
        assertEquals(emptyMap(), state.flags)
        assertEquals(RuleSetDraftStatus.Draft, state.status)
    }

    @Test
    fun `rejects draft created event when state already exists`() {
        val state = draftState()
        val event =
            RuleSetDraftCreated(
                eventId = eventId(),
                draftId = state.draftId,
                ruleSetVersion = version(1),
            )

        val actual = RuleManagementEventApplier.apply(state = state, event = event)

        assertEquals(Outcome.Err(RuleManagementEventApplyError.DraftAlreadyExists(state.draftId)), actual)
    }

    @Test
    fun `applies draft validated event`() {
        val state = draftState()
        val event =
            RuleSetDraftValidated(
                eventId = eventId(),
                draftId = state.draftId,
                ruleSetVersion = state.ruleSetVersion,
            )

        val actual = RuleManagementEventApplier.apply(state = state, event = event)

        assertEquals(
            RuleSetDraftStatus.Validated,
            (actual as Outcome.Ok)
                .value
                .status,
        )
    }

    @Test
    fun `rejects event when draft id differs from state`() {
        val state = draftState()
        val actualDraftId = draftId("018ff7c1-9354-7b02-b021-76d2791d6a24")
        val event =
            RuleSetDraftValidated(
                eventId = eventId(),
                draftId = actualDraftId,
                ruleSetVersion = state.ruleSetVersion,
            )

        val actual = RuleManagementEventApplier.apply(state = state, event = event)

        assertEquals(
            Outcome.Err(
                RuleManagementEventApplyError.DraftIdMismatch(
                    expected = state.draftId,
                    actual = actualDraftId,
                ),
            ),
            actual,
        )
    }
}
