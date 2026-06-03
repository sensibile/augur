@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package me.sensibile.augur.rule.eventsourcing

import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.management.RuleManagementEvent
import me.sensibile.augur.rule.management.RuleManagementEventStore
import me.sensibile.augur.rule.management.RuleManagementEventStoreError
import me.sensibile.augur.rule.management.RuleManagementEventStream
import me.sensibile.augur.rule.management.RuleManagementExpectedStreamVersion
import me.sensibile.augur.rule.management.RuleManagementStreamVersion
import me.sensibile.augur.rule.management.RuleSetDraftId
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventSourcingTemplate
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventStreamVersionConflictException

class BricksRuleManagementEventStore(
    private val events: EventSourcingTemplate,
    private val codec: RuleManagementEventCodec = JsonRuleManagementEventCodec,
) : RuleManagementEventStore {
    @Suppress("TooGenericExceptionCaught")
    override fun load(draftId: RuleSetDraftId): Outcome<RuleManagementEventStoreError, RuleManagementEventStream> =
        try {
            val storedEvents = events.load(draftId.toStreamId())
            val decodedEvents = mutableListOf<RuleManagementEvent>()

            for (storedEvent in storedEvents) {
                when (val decoded = codec.decode(storedEvent)) {
                    is Outcome.Err -> {
                        return Outcome.Err(RuleManagementEventStoreError.StorageFailure(decoded.error.message))
                    }

                    is Outcome.Ok -> {
                        decodedEvents += decoded.value
                    }
                }
            }

            Outcome.Ok(
                RuleManagementEventStream(
                    draftId = draftId,
                    version = storedEvents.lastOrNull()?.streamVersion?.toStreamVersion(),
                    events = decodedEvents.asSequence(),
                ),
            )
        } catch (exception: RuntimeException) {
            Outcome.Err(RuleManagementEventStoreError.StorageFailure(exception.message.orEmpty()))
        }

    @Suppress("TooGenericExceptionCaught")
    override fun append(
        expectedVersion: RuleManagementExpectedStreamVersion,
        event: RuleManagementEvent,
    ): Outcome<RuleManagementEventStoreError, RuleManagementStreamVersion> =
        try {
            val appended =
                events.append(
                    streamId = event.draftId.toStreamId(),
                    expectedVersion = expectedVersion.toBricksExpectedVersion(),
                    events = listOf(codec.encode(event)),
                )

            Outcome.Ok(appended.currentVersion.toStreamVersion())
        } catch (exception: EventStreamVersionConflictException) {
            Outcome.Err(
                RuleManagementEventStoreError.StreamVersionConflict(
                    draftId = event.draftId,
                    expected = expectedVersion,
                    actual = exception.actualVersion?.toStreamVersionOrNull(),
                ),
            )
        } catch (exception: RuntimeException) {
            Outcome.Err(RuleManagementEventStoreError.StorageFailure(exception.message.orEmpty()))
        }
}

internal fun RuleSetDraftId.toStreamId(): String = "$RULE_SET_DRAFT_STREAM_PREFIX$value"

internal fun String.toDraftIdFromStreamId(): Outcome<RuleManagementEventCodecError, RuleSetDraftId> {
    val draftId = removePrefix(RULE_SET_DRAFT_STREAM_PREFIX)
    if (draftId == this) {
        return Outcome.Err(RuleManagementEventCodecError.InvalidStreamId(this))
    }

    return parseUuidV7(field = "ruleSetDraftId", value = draftId, parse = RuleSetDraftId::of)
}

private fun RuleManagementExpectedStreamVersion.toBricksExpectedVersion(): Long =
    when (this) {
        RuleManagementExpectedStreamVersion.NoStream -> 0
        is RuleManagementExpectedStreamVersion.Exact -> version.value
    }

private fun Long.toStreamVersion(): RuleManagementStreamVersion =
    when (val version = RuleManagementStreamVersion.of(this)) {
        is Outcome.Err -> error("Invalid rule management stream version from event store: ${version.error}")
        is Outcome.Ok -> version.value
    }

private fun Long.toStreamVersionOrNull(): RuleManagementStreamVersion? =
    if (this == 0L) {
        null
    } else {
        toStreamVersion()
    }

private const val RULE_SET_DRAFT_STREAM_PREFIX = "rule-set-draft:"
