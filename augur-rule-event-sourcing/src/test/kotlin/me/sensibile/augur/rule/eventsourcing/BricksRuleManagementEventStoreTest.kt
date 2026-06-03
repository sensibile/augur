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
import me.sensibile.augur.rule.management.RuleManagementEventId
import me.sensibile.augur.rule.management.RuleManagementEventStoreError
import me.sensibile.augur.rule.management.RuleManagementEventStream
import me.sensibile.augur.rule.management.RuleManagementExpectedStreamVersion
import me.sensibile.augur.rule.management.RuleManagementStreamVersion
import me.sensibile.augur.rule.management.RuleSetDraftCreated
import me.sensibile.augur.rule.management.RuleSetDraftId
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventSourcingTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
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

    private fun ruleManagementEventStore(): BricksRuleManagementEventStore =
        BricksRuleManagementEventStore(EventSourcingTemplate(InMemoryBricksEventStore()))

    private fun flag(key: String): Flag =
        Flag(
            key = flagKey(key),
            enabled = true,
            defaultValue = RuleValue.boolean(false),
            rules =
                listOf(
                    Rule(
                        id = ruleId(),
                        condition =
                            Condition.Predicate(
                                attributeKey = attributeKey("country"),
                                operator = Operator.Eq,
                                value = RuleValue.string("KR"),
                            ),
                        serve = RuleValue.boolean(true),
                    ),
                ),
        )

    private fun draftId(value: String = "018ff7c1-9354-7b02-b021-76d2791d6a21"): RuleSetDraftId =
        RuleSetDraftId.of(Uuid.parse(value)).requireOk()

    private fun eventId(value: String = "018ff7c1-9354-7b02-b021-76d2791d6a22"): RuleManagementEventId =
        RuleManagementEventId.of(Uuid.parse(value)).requireOk()

    private fun ruleId(value: String = "018ff7c1-9354-7b02-b021-76d2791d6a23"): RuleId = RuleId.of(Uuid.parse(value)).requireOk()

    private fun ruleSetVersion(value: Long): RuleSetVersion = RuleSetVersion.of(value).requireOk()

    private fun streamVersion(value: Long): RuleManagementStreamVersion = RuleManagementStreamVersion.of(value).requireOk()

    private fun flagKey(value: String): FlagKey = FlagKey.of(value).requireOk()

    private fun attributeKey(value: String): AttributeKey = AttributeKey.of(value).requireOk()

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
