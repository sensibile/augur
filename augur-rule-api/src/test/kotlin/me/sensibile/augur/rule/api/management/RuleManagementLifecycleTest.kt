package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RuleManagementLifecycleTest {
    @Test
    fun `handles and applies draft publish lifecycle`() {
        val draftId = draftId()
        val flag = flag("new_checkout")
        val rule = rule()
        val publishedRuleSetId = publishedRuleSetId()
        var state: RuleSetDraftState? = null

        val created =
            handleAndApply(
                state = state,
                command = CreateRuleSetDraft(draftId = draftId, ruleSetVersion = version(1)),
                eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a22"),
            )
        assertIs<RuleSetDraftCreated>(created.event)
        state = created.state

        val flagAdded =
            handleAndApply(
                state = state,
                command = AddFlag(draftId = draftId, flag = flag),
                eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a26"),
            )
        assertIs<FlagAdded>(flagAdded.event)
        state = flagAdded.state

        val ruleAdded =
            handleAndApply(
                state = state,
                command = AddRule(draftId = draftId, flagKey = flag.key, rule = rule),
                eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a27"),
            )
        assertIs<RuleAdded>(ruleAdded.event)
        state = ruleAdded.state

        val validated =
            handleAndApply(
                state = state,
                command = ValidateRuleSetDraft(draftId = draftId),
                eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a28"),
            )
        assertIs<RuleSetDraftValidated>(validated.event)
        state = validated.state

        val published =
            handleAndApply(
                state = state,
                command = PublishRuleSet(draftId = draftId, publishedRuleSetId = publishedRuleSetId),
                eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a29"),
            )
        assertIs<RuleSetPublished>(published.event)
        state = published.state

        val archived =
            handleAndApply(
                state = state,
                command = ArchiveRuleSet(draftId = draftId),
                eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a2a"),
            )
        assertIs<RuleSetArchived>(archived.event)
        state = archived.state

        val finalState = requireNotNull(state)
        assertEquals(RuleSetDraftStatus.Archived, finalState.status)
        assertEquals(listOf(rule), finalState.flags.getValue(flag.key).rules)
    }

    private fun handleAndApply(
        state: RuleSetDraftState?,
        command: RuleManagementCommand,
        eventId: RuleManagementEventId,
    ): AppliedCommand {
        val event =
            when (val handled = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId)) {
                is Outcome.Err -> error("Command failed: ${handled.error}")
                is Outcome.Ok -> handled.value
            }
        val nextState =
            when (val applied = RuleManagementEventApplier.apply(state = state, event = event)) {
                is Outcome.Err -> error("Event apply failed: ${applied.error}")
                is Outcome.Ok -> applied.value
            }
        return AppliedCommand(event = event, state = nextState)
    }

    private data class AppliedCommand(
        val event: RuleManagementEvent,
        val state: RuleSetDraftState,
    )
}
