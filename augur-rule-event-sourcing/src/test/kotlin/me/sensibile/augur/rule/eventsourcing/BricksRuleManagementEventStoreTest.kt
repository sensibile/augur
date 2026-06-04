@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package me.sensibile.augur.rule.eventsourcing

import me.sensibile.augur.rule.AttributeKey
import me.sensibile.augur.rule.Condition
import me.sensibile.augur.rule.Flag
import me.sensibile.augur.rule.FlagKey
import me.sensibile.augur.rule.Operator
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.Rule
import me.sensibile.augur.rule.RuleId
import me.sensibile.augur.rule.RuleSetVersion
import me.sensibile.augur.rule.RuleValue
import me.sensibile.augur.rule.management.FlagAdded
import me.sensibile.augur.rule.management.FlagDisabled
import me.sensibile.augur.rule.management.FlagEnabled
import me.sensibile.augur.rule.management.PublishedRuleSetId
import me.sensibile.augur.rule.management.RuleAdded
import me.sensibile.augur.rule.management.RuleConditionChanged
import me.sensibile.augur.rule.management.RuleManagementEvent
import me.sensibile.augur.rule.management.RuleManagementEventId
import me.sensibile.augur.rule.management.RuleManagementEventStoreError
import me.sensibile.augur.rule.management.RuleManagementEventStream
import me.sensibile.augur.rule.management.RuleManagementExpectedStreamVersion
import me.sensibile.augur.rule.management.RuleManagementStreamVersion
import me.sensibile.augur.rule.management.RuleRemoved
import me.sensibile.augur.rule.management.RuleServeValueChanged
import me.sensibile.augur.rule.management.RuleSetArchived
import me.sensibile.augur.rule.management.RuleSetDraftCreated
import me.sensibile.augur.rule.management.RuleSetDraftId
import me.sensibile.augur.rule.management.RuleSetDraftValidated
import me.sensibile.augur.rule.management.RuleSetPublished
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventAppendResult
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventSourcingTemplate
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventStore
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventStoreEvent
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.StoredEvent
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.uuid.Uuid

class BricksRuleManagementEventStoreTest {
    @Test
    fun `appends and loads rule management events through bricks event store`() {
        val store = ruleManagementEventStore()
        val draftId = draftId()
        val created =
            RuleSetDraftCreated(
                eventId = eventId(),
                draftId = draftId,
                ruleSetVersion = ruleSetVersion(1),
            )
        val flagAdded =
            FlagAdded(
                eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a26"),
                draftId = draftId,
                flag = flag("new_checkout"),
            )

        assertEquals(Outcome.Ok(streamVersion(1)), store.append(RuleManagementExpectedStreamVersion.NoStream, created))
        assertEquals(
            Outcome.Ok(streamVersion(2)),
            store.append(RuleManagementExpectedStreamVersion.Exact(streamVersion(1)), flagAdded),
        )

        val loaded = store.load(draftId)

        val expected =
            Outcome.Ok(
                RuleManagementEventStream(
                    draftId = draftId,
                    version = streamVersion(2),
                    events = sequenceOf(created, flagAdded),
                ),
            )
        assertEquals(expected.valueOrNull()?.copy(events = emptySequence()), loaded.valueOrNull()?.copy(events = emptySequence()))
        assertEquals(listOf(created, flagAdded), loaded.valueOrNull()?.events?.toList())
    }

    @Test
    fun `round trips every rule management event type through json codec`() {
        val store = ruleManagementEventStore()
        val draftId = draftId()
        val flagKey = flagKey("new_checkout")
        val ruleId = ruleId()
        val events =
            listOf(
                RuleSetDraftCreated(eventId("018ff7c1-9354-7b02-b021-76d2791d6a22"), draftId, ruleSetVersion(1)),
                FlagAdded(eventId("018ff7c1-9354-7b02-b021-76d2791d6a26"), draftId, flag(flagKey.value)),
                FlagEnabled(eventId("018ff7c1-9354-7b02-b021-76d2791d6a27"), draftId, flagKey),
                FlagDisabled(eventId("018ff7c1-9354-7b02-b021-76d2791d6a28"), draftId, flagKey),
                RuleAdded(eventId("018ff7c1-9354-7b02-b021-76d2791d6a29"), draftId, flagKey, rule(ruleId)),
                RuleConditionChanged(
                    eventId("018ff7c1-9354-7b02-b021-76d2791d6a2a"),
                    draftId,
                    flagKey,
                    ruleId,
                    condition("plan", Operator.In, RuleValue.list(listOf(RuleValue.string("pro")))),
                ),
                RuleServeValueChanged(
                    eventId("018ff7c1-9354-7b02-b021-76d2791d6a2b"),
                    draftId,
                    flagKey,
                    ruleId,
                    RuleValue.boolean(false),
                ),
                RuleRemoved(eventId("018ff7c1-9354-7b02-b021-76d2791d6a2c"), draftId, flagKey, ruleId),
                RuleSetDraftValidated(eventId("018ff7c1-9354-7b02-b021-76d2791d6a2d"), draftId, ruleSetVersion(1)),
                RuleSetPublished(
                    eventId("018ff7c1-9354-7b02-b021-76d2791d6a2e"),
                    draftId,
                    publishedRuleSetId(),
                    ruleSetVersion(1),
                ),
                RuleSetArchived(eventId("018ff7c1-9354-7b02-b021-76d2791d6a2f"), draftId),
            )

        appendAll(store, events)

        val loaded = store.load(draftId)

        assertEquals(events, loaded.valueOrNull()?.events?.toList())
        assertEquals(streamVersion(events.size.toLong()), loaded.valueOrNull()?.version)
    }

    @Test
    fun `maps bricks version conflict to rule management store conflict`() {
        val store = ruleManagementEventStore()
        val event =
            RuleSetDraftCreated(
                eventId = eventId(),
                draftId = draftId(),
                ruleSetVersion = ruleSetVersion(1),
            )

        val actual = store.append(RuleManagementExpectedStreamVersion.Exact(streamVersion(1)), event)

        assertEquals(
            Outcome.Err(
                RuleManagementEventStoreError.StreamVersionConflict(
                    draftId = event.draftId,
                    expected = RuleManagementExpectedStreamVersion.Exact(streamVersion(1)),
                    actual = null,
                ),
            ),
            actual,
        )
    }

    @Test
    fun `maps bricks version conflict with actual version to stream version`() {
        val store = ruleManagementEventStore()
        val draftId = draftId()
        val created =
            RuleSetDraftCreated(
                eventId = eventId(),
                draftId = draftId,
                ruleSetVersion = ruleSetVersion(1),
            )
        val nextCreated =
            created.copy(eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a26"))
        val appended = store.append(RuleManagementExpectedStreamVersion.NoStream, created)
        assertEquals(streamVersion(1), (appended as Outcome.Ok).value)

        val actual = store.append(RuleManagementExpectedStreamVersion.NoStream, nextCreated)

        assertEquals(
            Outcome.Err(
                RuleManagementEventStoreError.StreamVersionConflict(
                    draftId = draftId,
                    expected = RuleManagementExpectedStreamVersion.NoStream,
                    actual = streamVersion(1),
                ),
            ),
            actual,
        )
    }

    @Test
    fun `maps stored event decode failure to storage failure when loading`() {
        val bricksStore = InMemoryBricksEventStore()
        val draftId = draftId()
        bricksStore.append(
            streamId = draftId.toStreamId(),
            expectedVersion = 0,
            events =
                listOf(
                    EventStoreEvent(
                        id = eventId().value.toString(),
                        eventType = "unknown.event",
                        eventVersion = 1,
                        payloadJson = "{}",
                    ),
                ),
        )
        val store = BricksRuleManagementEventStore(EventSourcingTemplate(bricksStore))

        val actual = store.load(draftId)

        val error = assertIs<RuleManagementEventStoreError.StorageFailure>((actual as Outcome.Err).error)
        assertEquals("Unsupported rule management event type: unknown.event", error.message)
    }

    @Test
    fun `maps runtime failures from bricks store to storage failure`() {
        val loadStore =
            BricksRuleManagementEventStore(
                EventSourcingTemplate(FailingBricksEventStore(loadMessage = "load failed")),
            )
        val appendStore =
            BricksRuleManagementEventStore(
                EventSourcingTemplate(FailingBricksEventStore(appendMessage = "append failed")),
            )
        val event =
            RuleSetDraftCreated(
                eventId = eventId(),
                draftId = draftId(),
                ruleSetVersion = ruleSetVersion(1),
            )

        assertEquals(
            Outcome.Err(RuleManagementEventStoreError.StorageFailure("load failed")),
            loadStore.load(event.draftId),
        )
        assertEquals(
            Outcome.Err(RuleManagementEventStoreError.StorageFailure("append failed")),
            appendStore.append(RuleManagementExpectedStreamVersion.NoStream, event),
        )
    }

    @Test
    fun `returns codec error when stored event has unsupported type`() {
        val actual =
            JsonRuleManagementEventCodec.decode(
                storedEvent(
                    eventType = "unknown.event",
                    payloadJson = "{}",
                ),
            )

        assertEquals(
            Outcome.Err(RuleManagementEventCodecError.UnsupportedEventType("unknown.event")),
            actual,
        )
    }

    @Test
    fun `returns codec error when event id is malformed`() {
        val actual =
            JsonRuleManagementEventCodec.decode(
                storedEvent(
                    id = "not-a-uuid",
                    eventType = "rule-set-draft.created",
                    payloadJson = """{"ruleSetVersion":1}""",
                ),
            )

        val error = assertIs<RuleManagementEventCodecError.InvalidPayload>((actual as Outcome.Err).error)
        assertEquals("ruleManagementEventId", error.eventType)
    }

    @Test
    fun `returns codec error when payload json is malformed`() {
        val actual =
            JsonRuleManagementEventCodec.decode(
                storedEvent(
                    eventType = "rule-set-draft.created",
                    payloadJson = """{"ruleSetVersion":""",
                ),
            )

        val error = assertIs<RuleManagementEventCodecError.InvalidPayload>((actual as Outcome.Err).error)
        assertEquals("rule-set-draft.created", error.eventType)
    }

    @Test
    fun `returns codec error when payload value object is invalid`() {
        val actual =
            JsonRuleManagementEventCodec.decode(
                storedEvent(
                    eventType = "flag.enabled",
                    payloadJson = """{"flagKey":"Invalid Key"}""",
                ),
            )

        val error = assertIs<RuleManagementEventCodecError.InvalidPayload>((actual as Outcome.Err).error)
        assertEquals("flag.enabled", error.eventType)
    }

    @Test
    fun `returns codec error when stored event stream id is not a draft stream`() {
        val actual =
            JsonRuleManagementEventCodec.decode(
                storedEvent(
                    streamId = "other:018ff7c1-9354-7b02-b021-76d2791d6a21",
                    eventType = "rule-set-draft.created",
                    payloadJson = """{"ruleSetVersion":1}""",
                ),
            )

        assertEquals(
            Outcome.Err(RuleManagementEventCodecError.InvalidStreamId("other:018ff7c1-9354-7b02-b021-76d2791d6a21")),
            actual,
        )
    }

    private fun appendAll(
        store: BricksRuleManagementEventStore,
        events: List<RuleManagementEvent>,
    ) {
        events.fold<RuleManagementEvent, RuleManagementExpectedStreamVersion>(
            RuleManagementExpectedStreamVersion.NoStream,
        ) { expectedVersion, event ->
            val appended = store.append(expectedVersion, event)
            RuleManagementExpectedStreamVersion.Exact((appended as Outcome.Ok).value)
        }
    }

    private fun ruleManagementEventStore(): BricksRuleManagementEventStore =
        BricksRuleManagementEventStore(EventSourcingTemplate(InMemoryBricksEventStore()))

    private fun rule(id: RuleId): Rule =
        Rule(
            id = id,
            condition = condition("country", Operator.Eq, RuleValue.string("KR")),
            serve = RuleValue.boolean(true),
        )

    private fun condition(
        attributeKey: String,
        operator: Operator,
        value: RuleValue,
    ): Condition =
        Condition.Predicate(
            attributeKey = attributeKey(attributeKey),
            operator = operator,
            value = value,
        )

    private fun flag(key: String): Flag =
        Flag(
            key = flagKey(key),
            enabled = true,
            defaultValue = RuleValue.boolean(false),
            rules =
                listOf(
                    Rule(
                        id = ruleId(),
                        condition = condition("country", Operator.Eq, RuleValue.string("KR")),
                        serve = RuleValue.boolean(true),
                    ),
                ),
        )

    private fun draftId(value: String = "018ff7c1-9354-7b02-b021-76d2791d6a21"): RuleSetDraftId =
        RuleSetDraftId.of(Uuid.parse(value)).requireOk()

    private fun eventId(value: String = "018ff7c1-9354-7b02-b021-76d2791d6a22"): RuleManagementEventId =
        RuleManagementEventId.of(Uuid.parse(value)).requireOk()

    private fun publishedRuleSetId(value: String = "018ff7c1-9354-7b02-b021-76d2791d6a25"): PublishedRuleSetId =
        PublishedRuleSetId.of(Uuid.parse(value)).requireOk()

    private fun ruleId(value: String = "018ff7c1-9354-7b02-b021-76d2791d6a23"): RuleId = RuleId.of(Uuid.parse(value)).requireOk()

    private fun ruleSetVersion(value: Long): RuleSetVersion = RuleSetVersion.of(value).requireOk()

    private fun streamVersion(value: Long): RuleManagementStreamVersion = RuleManagementStreamVersion.of(value).requireOk()

    private fun flagKey(value: String): FlagKey = FlagKey.of(value).requireOk()

    private fun attributeKey(value: String): AttributeKey = AttributeKey.of(value).requireOk()

    private fun storedEvent(
        id: String = "018ff7c1-9354-7b02-b021-76d2791d6a22",
        streamId: String = "rule-set-draft:018ff7c1-9354-7b02-b021-76d2791d6a21",
        eventType: String,
        payloadJson: String,
    ): StoredEvent =
        StoredEvent(
            id = id,
            streamId = streamId,
            streamVersion = 1,
            eventType = eventType,
            payloadJson = payloadJson,
            occurredAt = Instant.EPOCH,
        )

    private fun <E, A> Outcome<E, A>.requireOk(): A =
        when (this) {
            is Outcome.Err -> error("Expected Ok but got $error")
            is Outcome.Ok -> value
        }

    private fun <E, A> Outcome<E, A>.valueOrNull(): A? =
        when (this) {
            is Outcome.Err -> null
            is Outcome.Ok -> value
        }
}

private class FailingBricksEventStore(
    private val loadMessage: String = "load failed",
    private val appendMessage: String = "append failed",
) : EventStore {
    override fun append(
        streamId: String,
        expectedVersion: Long,
        events: List<EventStoreEvent>,
    ): EventAppendResult = throw IllegalStateException(appendMessage)

    override fun load(
        streamId: String,
        fromVersion: Long,
    ): List<StoredEvent> = throw IllegalStateException(loadMessage)
}
