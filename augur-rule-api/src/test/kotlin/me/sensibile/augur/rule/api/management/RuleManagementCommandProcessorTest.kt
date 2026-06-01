package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RuleManagementCommandProcessorTest {
    @Test
    fun `handles command and applies emitted event`() {
        val draftId = draftId()
        val command = CreateRuleSetDraft(draftId = draftId, ruleSetVersion = version(1))

        val actual = RuleManagementCommandProcessor.process(state = null, command = command, eventId = eventId())

        val result = (actual as Outcome.Ok).value
        assertIs<RuleSetDraftCreated>(result.event)
        assertEquals(draftId, result.state.draftId)
        assertEquals(RuleSetDraftStatus.Draft, result.state.status)
    }

    @Test
    fun `returns command rejected when command cannot be handled`() {
        val state = draftState()
        val command = CreateRuleSetDraft(draftId = state.draftId, ruleSetVersion = version(1))

        val actual = RuleManagementCommandProcessor.process(state = state, command = command, eventId = eventId())

        assertEquals(
            Outcome.Err(
                RuleManagementProcessError.CommandRejected(
                    RuleManagementCommandError.DraftAlreadyExists(state.draftId),
                ),
            ),
            actual,
        )
    }

    @Test
    fun `rejects command before applying event when draft id mismatches`() {
        val state = draftState()
        val actualDraftId = draftId("018ff7c1-9354-7b02-b021-76d2791d6a24")
        val command = AddFlag(draftId = actualDraftId, flag = flag("new_checkout"))

        val actual = RuleManagementCommandProcessor.process(state = state, command = command, eventId = eventId())

        assertEquals(
            Outcome.Err(
                RuleManagementProcessError.CommandRejected(
                    RuleManagementCommandError.DraftIdMismatch(
                        expected = state.draftId,
                        actual = actualDraftId,
                    ),
                ),
            ),
            actual,
        )
    }
}
