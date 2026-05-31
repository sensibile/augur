package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RuleManagementDraftCommandHandlerTest {
    @Test
    fun `creates draft when no state exists`() {
        val draftId = draftId()
        val eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a22")
        val command = CreateRuleSetDraft(draftId = draftId, ruleSetVersion = version(1))

        val actual = RuleManagementCommandHandler.handle(state = null, command = command, eventId = eventId)

        assertEquals(
            Outcome.Ok(
                RuleSetDraftCreated(
                    eventId = eventId,
                    draftId = draftId,
                    ruleSetVersion = version(1),
                ),
            ),
            actual,
        )
    }

    @Test
    fun `rejects create draft when state already exists`() {
        val state = draftState()
        val command = CreateRuleSetDraft(draftId = state.draftId, ruleSetVersion = version(1))

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId())

        assertEquals(Outcome.Err(RuleManagementCommandError.DraftAlreadyExists(state.draftId)), actual)
    }

    @Test
    fun `validates valid draft`() {
        val state = draftState(flags = mapOf(flagKey("new_checkout") to flag("new_checkout")))
        val eventId = eventId()
        val command = ValidateRuleSetDraft(draftId = state.draftId)

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId)

        assertEquals(
            Outcome.Ok(
                RuleSetDraftValidated(
                    eventId = eventId,
                    draftId = state.draftId,
                    ruleSetVersion = state.ruleSetVersion,
                ),
            ),
            actual,
        )
    }

    @Test
    fun `rejects invalid draft`() {
        val state =
            draftState(
                flags =
                    mapOf(
                        flagKey("new_checkout") to
                            flag(
                                key = "wrong_key",
                            ),
                    ),
            )
        val command = ValidateRuleSetDraft(draftId = state.draftId)

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId())

        assertIs<Outcome.Err<RuleManagementCommandError.InvalidRuleSetDraft>>(actual)
    }
}
