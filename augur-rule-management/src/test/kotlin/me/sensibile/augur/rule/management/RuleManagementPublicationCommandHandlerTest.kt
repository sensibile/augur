package me.sensibile.augur.rule.management

import me.sensibile.augur.rule.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals

class RuleManagementPublicationCommandHandlerTest {
    @Test
    fun `publishes validated draft`() {
        val state = draftState(status = RuleSetDraftStatus.Validated)
        val eventId = eventId()
        val publishedRuleSetId = publishedRuleSetId()
        val command = PublishRuleSet(draftId = state.draftId, publishedRuleSetId = publishedRuleSetId)

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId)

        assertEquals(
            Outcome.Ok(
                RuleSetPublished(
                    eventId = eventId,
                    draftId = state.draftId,
                    publishedRuleSetId = publishedRuleSetId,
                    ruleSetVersion = state.ruleSetVersion,
                ),
            ),
            actual,
        )
    }

    @Test
    fun `rejects publish when draft is not validated`() {
        val state = draftState(status = RuleSetDraftStatus.Draft)
        val command = PublishRuleSet(draftId = state.draftId, publishedRuleSetId = publishedRuleSetId())

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId())

        assertEquals(
            Outcome.Err(RuleManagementCommandError.DraftIsNotPublishable(state.draftId, RuleSetDraftStatus.Draft)),
            actual,
        )
    }

    @Test
    fun `archives published draft`() {
        val state = draftState(status = RuleSetDraftStatus.Published)
        val eventId = eventId()
        val command = ArchiveRuleSet(draftId = state.draftId)

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId)

        assertEquals(
            Outcome.Ok(RuleSetArchived(eventId = eventId, draftId = state.draftId)),
            actual,
        )
    }

    @Test
    fun `rejects archive when draft is not published`() {
        val state = draftState(status = RuleSetDraftStatus.Validated)
        val command = ArchiveRuleSet(draftId = state.draftId)

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId())

        assertEquals(
            Outcome.Err(RuleManagementCommandError.DraftIsNotArchivable(state.draftId, RuleSetDraftStatus.Validated)),
            actual,
        )
    }
}
