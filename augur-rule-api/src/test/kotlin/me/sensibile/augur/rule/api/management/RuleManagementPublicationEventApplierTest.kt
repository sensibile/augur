package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals

class RuleManagementPublicationEventApplierTest {
    @Test
    fun `applies rule set published event`() {
        val state = draftState(status = RuleSetDraftStatus.Validated)
        val event =
            RuleSetPublished(
                eventId = eventId(),
                draftId = state.draftId,
                publishedRuleSetId = publishedRuleSetId(),
                ruleSetVersion = state.ruleSetVersion,
            )

        val actual = RuleManagementEventApplier.apply(state = state, event = event)

        assertEquals(
            RuleSetDraftStatus.Published,
            (actual as Outcome.Ok)
                .value
                .status,
        )
    }

    @Test
    fun `applies rule set archived event`() {
        val state = draftState(status = RuleSetDraftStatus.Published)
        val event =
            RuleSetArchived(
                eventId = eventId(),
                draftId = state.draftId,
            )

        val actual = RuleManagementEventApplier.apply(state = state, event = event)

        assertEquals(
            RuleSetDraftStatus.Archived,
            (actual as Outcome.Ok)
                .value
                .status,
        )
    }
}
