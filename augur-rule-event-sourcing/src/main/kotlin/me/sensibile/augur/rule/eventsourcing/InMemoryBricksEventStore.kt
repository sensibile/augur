package me.sensibile.augur.rule.eventsourcing

import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventAppendResult
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventStore
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventStoreEvent
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventStreamVersionConflictException
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.StoredEvent
import java.util.concurrent.ConcurrentHashMap

class InMemoryBricksEventStore : EventStore {
    private val streams = ConcurrentHashMap<String, MutableList<StoredEvent>>()

    override fun append(
        streamId: String,
        expectedVersion: Long,
        events: List<EventStoreEvent>,
    ): EventAppendResult =
        synchronized(this) {
            require(streamId.isNotBlank()) { "streamId must not be blank." }
            require(expectedVersion >= 0) { "expectedVersion must not be negative." }
            require(events.isNotEmpty()) { "events must not be empty." }

            val currentEvents = streams[streamId]
            val actualVersion = currentEvents?.lastOrNull()?.streamVersion ?: 0
            if (actualVersion != expectedVersion) {
                throw EventStreamVersionConflictException(
                    streamId = streamId,
                    expectedVersion = expectedVersion,
                    actualVersion = actualVersion,
                )
            }

            val storedEvents =
                events.mapIndexed { index, event ->
                    event.toStoredEvent(
                        streamId = streamId,
                        streamVersion = expectedVersion + index + 1,
                    )
                }
            val stream = currentEvents ?: mutableListOf<StoredEvent>().also { streams[streamId] = it }
            stream += storedEvents

            EventAppendResult(
                streamId = streamId,
                previousVersion = expectedVersion,
                currentVersion = storedEvents.last().streamVersion,
                events = storedEvents,
            )
        }

    override fun load(
        streamId: String,
        fromVersion: Long,
    ): List<StoredEvent> =
        synchronized(this) {
            require(streamId.isNotBlank()) { "streamId must not be blank." }
            require(fromVersion >= 1) { "fromVersion must be positive." }

            streams[streamId]
                ?.filter { event -> event.streamVersion >= fromVersion }
                .orEmpty()
        }

    private fun EventStoreEvent.toStoredEvent(
        streamId: String,
        streamVersion: Long,
    ): StoredEvent =
        StoredEvent(
            id = id,
            streamId = streamId,
            streamVersion = streamVersion,
            eventType = eventType,
            eventVersion = eventVersion,
            payloadJson = payloadJson,
            metadataJson = metadataJson,
            occurredAt = occurredAt,
        )
}
