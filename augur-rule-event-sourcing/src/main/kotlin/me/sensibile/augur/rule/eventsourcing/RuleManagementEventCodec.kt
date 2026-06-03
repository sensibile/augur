@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package me.sensibile.augur.rule.eventsourcing

import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.ValueObjectError
import me.sensibile.augur.rule.management.RuleManagementEvent
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventStoreEvent
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.StoredEvent
import kotlin.uuid.Uuid

interface RuleManagementEventCodec {
    fun encode(event: RuleManagementEvent): EventStoreEvent

    fun decode(event: StoredEvent): Outcome<RuleManagementEventCodecError, RuleManagementEvent>
}

sealed interface RuleManagementEventCodecError {
    val message: String

    data class InvalidStreamId(
        val streamId: String,
    ) : RuleManagementEventCodecError {
        override val message: String = "Invalid rule management stream id: $streamId"
    }

    data class InvalidValueObject(
        val error: ValueObjectError,
    ) : RuleManagementEventCodecError {
        override val message: String = "Invalid rule management event payload value: $error"
    }

    data class InvalidPayload(
        val eventType: String,
        val reason: String,
    ) : RuleManagementEventCodecError {
        override val message: String = "Invalid rule management event payload. eventType=$eventType, reason=$reason"
    }

    data class UnsupportedEventType(
        val eventType: String,
    ) : RuleManagementEventCodecError {
        override val message: String = "Unsupported rule management event type: $eventType"
    }
}

internal inline fun <A> parseUuidV7(
    field: String,
    value: String,
    parse: (Uuid) -> Outcome<ValueObjectError, A>,
): Outcome<RuleManagementEventCodecError, A> {
    val uuid =
        try {
            Uuid.parse(value)
        } catch (exception: IllegalArgumentException) {
            return Outcome.Err(RuleManagementEventCodecError.InvalidPayload(field, exception.message.orEmpty()))
        }

    return when (val parsed = parse(uuid)) {
        is Outcome.Err -> Outcome.Err(RuleManagementEventCodecError.InvalidValueObject(parsed.error))
        is Outcome.Ok -> Outcome.Ok(parsed.value)
    }
}
