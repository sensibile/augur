package me.sensibile.augur.rule.management

import me.sensibile.augur.rule.Outcome

class RuleManagementCommandService(
    private val eventStore: RuleManagementEventStore,
) {
    fun handle(
        command: RuleManagementCommand,
        eventId: RuleManagementEventId,
    ): Outcome<RuleManagementCommandServiceError, RuleManagementStoredCommandResult> =
        when (val loaded = eventStore.load(command.draftId)) {
            is Outcome.Err -> {
                Outcome.Err(RuleManagementCommandServiceError.EventStoreLoadFailed(loaded.error))
            }

            is Outcome.Ok -> {
                handleLoadedStream(command = command, eventId = eventId, stream = loaded.value)
            }
        }

    private fun handleLoadedStream(
        command: RuleManagementCommand,
        eventId: RuleManagementEventId,
        stream: RuleManagementEventStream,
    ): Outcome<RuleManagementCommandServiceError, RuleManagementStoredCommandResult> =
        when (val replayed = RuleManagementEventReplayer.replay(stream.events)) {
            is Outcome.Err -> {
                Outcome.Err(RuleManagementCommandServiceError.EventReplayFailed(replayed.error))
            }

            is Outcome.Ok -> {
                processCommand(state = replayed.value, command = command, eventId = eventId, streamVersion = stream.version)
            }
        }

    private fun processCommand(
        state: RuleSetDraftState?,
        command: RuleManagementCommand,
        eventId: RuleManagementEventId,
        streamVersion: RuleManagementStreamVersion?,
    ): Outcome<RuleManagementCommandServiceError, RuleManagementStoredCommandResult> =
        when (val processed = RuleManagementCommandProcessor.process(state = state, command = command, eventId = eventId)) {
            is Outcome.Err -> {
                Outcome.Err(RuleManagementCommandServiceError.CommandProcessFailed(processed.error))
            }

            is Outcome.Ok -> {
                appendEvent(result = processed.value, expectedVersion = streamVersion.toExpectedVersion())
            }
        }

    private fun appendEvent(
        result: RuleManagementCommandResult,
        expectedVersion: RuleManagementExpectedStreamVersion,
    ): Outcome<RuleManagementCommandServiceError, RuleManagementStoredCommandResult> =
        when (val appended = eventStore.append(expectedVersion = expectedVersion, event = result.event)) {
            is Outcome.Err -> {
                Outcome.Err(RuleManagementCommandServiceError.EventStoreAppendFailed(appended.error))
            }

            is Outcome.Ok -> {
                Outcome.Ok(
                    RuleManagementStoredCommandResult(
                        event = result.event,
                        state = result.state,
                        streamVersion = appended.value,
                    ),
                )
            }
        }

    private fun RuleManagementStreamVersion?.toExpectedVersion(): RuleManagementExpectedStreamVersion =
        if (this == null) {
            RuleManagementExpectedStreamVersion.NoStream
        } else {
            RuleManagementExpectedStreamVersion.Exact(this)
        }
}

data class RuleManagementStoredCommandResult(
    val event: RuleManagementEvent,
    val state: RuleSetDraftState,
    val streamVersion: RuleManagementStreamVersion,
)

sealed interface RuleManagementCommandServiceError {
    data class EventStoreLoadFailed(
        val error: RuleManagementEventStoreError,
    ) : RuleManagementCommandServiceError

    data class EventReplayFailed(
        val error: RuleManagementEventApplyError,
    ) : RuleManagementCommandServiceError

    data class CommandProcessFailed(
        val error: RuleManagementProcessError,
    ) : RuleManagementCommandServiceError

    data class EventStoreAppendFailed(
        val error: RuleManagementEventStoreError,
    ) : RuleManagementCommandServiceError
}
