package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals

class RuleManagementFlagCommandHandlerTest {
    @Test
    fun `adds flag to editable draft`() {
        val state = draftState()
        val eventId = eventId()
        val flag = flag("new_checkout")
        val command = AddFlag(draftId = state.draftId, flag = flag)

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId)

        assertEquals(
            Outcome.Ok(FlagAdded(eventId = eventId, draftId = state.draftId, flag = flag)),
            actual,
        )
    }

    @Test
    fun `rejects duplicate flag`() {
        val flag = flag("new_checkout")
        val state = draftState(flags = mapOf(flag.key to flag))
        val command = AddFlag(draftId = state.draftId, flag = flag)

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId())

        assertEquals(
            Outcome.Err(RuleManagementCommandError.FlagAlreadyExists(state.draftId, flag.key)),
            actual,
        )
    }

    @Test
    fun `enables flag in editable draft`() {
        val flag = flag("new_checkout", enabled = false)
        val state = draftState(flags = mapOf(flag.key to flag))
        val eventId = eventId()
        val command = EnableFlag(draftId = state.draftId, flagKey = flag.key)

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId)

        assertEquals(
            Outcome.Ok(FlagEnabled(eventId = eventId, draftId = state.draftId, flagKey = flag.key)),
            actual,
        )
    }

    @Test
    fun `disables flag in editable draft`() {
        val flag = flag("new_checkout", enabled = true)
        val state = draftState(flags = mapOf(flag.key to flag))
        val eventId = eventId()
        val command = DisableFlag(draftId = state.draftId, flagKey = flag.key)

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId)

        assertEquals(
            Outcome.Ok(FlagDisabled(eventId = eventId, draftId = state.draftId, flagKey = flag.key)),
            actual,
        )
    }

    @Test
    fun `rejects flag status change when flag does not exist`() {
        val state = draftState()
        val flagKey = flagKey("new_checkout")
        val command = EnableFlag(draftId = state.draftId, flagKey = flagKey)

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId())

        assertEquals(
            Outcome.Err(RuleManagementCommandError.FlagNotFound(state.draftId, flagKey)),
            actual,
        )
    }

    @Test
    fun `rejects flag status change when draft is not editable`() {
        val flag = flag("new_checkout")
        val state =
            draftState(
                flags = mapOf(flag.key to flag),
                status = RuleSetDraftStatus.Validated,
            )
        val command = DisableFlag(draftId = state.draftId, flagKey = flag.key)

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId())

        assertEquals(
            Outcome.Err(RuleManagementCommandError.DraftIsNotEditable(state.draftId, RuleSetDraftStatus.Validated)),
            actual,
        )
    }
}
