package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.Outcome

object RuleManagementCommandProcessor {
    fun process(
        state: RuleSetDraftState?,
        command: RuleManagementCommand,
        eventId: RuleManagementEventId,
    ): Outcome<RuleManagementProcessError, RuleManagementCommandResult> =
        when (val handled = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId)) {
            is Outcome.Err -> {
                Outcome.Err(RuleManagementProcessError.CommandRejected(handled.error))
            }

            is Outcome.Ok -> {
                applyHandledEvent(state = state, event = handled.value)
            }
        }

    private fun applyHandledEvent(
        state: RuleSetDraftState?,
        event: RuleManagementEvent,
    ): Outcome<RuleManagementProcessError, RuleManagementCommandResult> =
        when (val applied = RuleManagementEventApplier.apply(state = state, event = event)) {
            is Outcome.Err -> {
                Outcome.Err(RuleManagementProcessError.EventApplyFailed(applied.error))
            }

            is Outcome.Ok -> {
                Outcome.Ok(RuleManagementCommandResult(event = event, state = applied.value))
            }
        }
}

data class RuleManagementCommandResult(
    val event: RuleManagementEvent,
    val state: RuleSetDraftState,
)

sealed interface RuleManagementProcessError {
    data class CommandRejected(
        val error: RuleManagementCommandError,
    ) : RuleManagementProcessError

    data class EventApplyFailed(
        val error: RuleManagementEventApplyError,
    ) : RuleManagementProcessError
}
