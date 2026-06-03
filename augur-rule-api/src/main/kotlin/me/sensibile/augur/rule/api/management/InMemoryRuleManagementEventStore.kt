package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.management.RuleManagementEvent
import me.sensibile.augur.rule.management.RuleManagementEventStore
import me.sensibile.augur.rule.management.RuleManagementEventStoreError
import me.sensibile.augur.rule.management.RuleManagementEventStream
import me.sensibile.augur.rule.management.RuleManagementExpectedStreamVersion
import me.sensibile.augur.rule.management.RuleManagementStreamVersion
import me.sensibile.augur.rule.management.RuleSetDraftId
import java.util.concurrent.ConcurrentHashMap

class InMemoryRuleManagementEventStore : RuleManagementEventStore {
    private val streams = ConcurrentHashMap<RuleSetDraftId, MutableList<RuleManagementEvent>>()

    override fun load(draftId: RuleSetDraftId): Outcome<RuleManagementEventStoreError, RuleManagementEventStream> =
        synchronized(this) {
            val events = streams[draftId]?.toList().orEmpty()
            Outcome.Ok(
                RuleManagementEventStream(
                    draftId = draftId,
                    version = events.size.toStreamVersionOrNull(),
                    events = events.asSequence(),
                ),
            )
        }

    override fun append(
        expectedVersion: RuleManagementExpectedStreamVersion,
        event: RuleManagementEvent,
    ): Outcome<RuleManagementEventStoreError, RuleManagementStreamVersion> =
        synchronized(this) {
            val events = streams[event.draftId]
            val actual = events?.size?.toStreamVersionOrNull()

            if (!expectedVersion.matches(actual)) {
                return@synchronized Outcome.Err(
                    RuleManagementEventStoreError.StreamVersionConflict(
                        draftId = event.draftId,
                        expected = expectedVersion,
                        actual = actual,
                    ),
                )
            }

            val stream = events ?: mutableListOf<RuleManagementEvent>().also { streams[event.draftId] = it }
            stream += event
            Outcome.Ok(stream.size.toStreamVersion())
        }

    private fun RuleManagementExpectedStreamVersion.matches(actual: RuleManagementStreamVersion?): Boolean =
        when (this) {
            RuleManagementExpectedStreamVersion.NoStream -> actual == null
            is RuleManagementExpectedStreamVersion.Exact -> version == actual
        }

    private fun Int.toStreamVersionOrNull(): RuleManagementStreamVersion? =
        if (this == 0) {
            null
        } else {
            toStreamVersion()
        }

    private fun Int.toStreamVersion(): RuleManagementStreamVersion =
        when (val version = RuleManagementStreamVersion.of(toLong())) {
            is Outcome.Err -> error("Invalid in-memory stream version: ${version.error}")
            is Outcome.Ok -> version.value
        }
}
