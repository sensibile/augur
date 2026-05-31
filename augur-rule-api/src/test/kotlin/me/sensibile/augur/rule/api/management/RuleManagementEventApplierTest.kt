package me.sensibile.augur.rule.api.management

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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class RuleManagementEventApplierTest {
    @Test
    fun `applies draft created event`() {
        val event =
            RuleSetDraftCreated(
                eventId = eventId(),
                draftId = draftId(),
                ruleSetVersion = version(1),
            )

        val actual = RuleManagementEventApplier.apply(state = null, event = event)

        val state = (actual as Outcome.Ok).value
        assertEquals(event.draftId, state.draftId)
        assertEquals(event.ruleSetVersion, state.ruleSetVersion)
        assertEquals(emptyMap(), state.flags)
        assertEquals(RuleSetDraftStatus.Draft, state.status)
    }

    @Test
    fun `applies flag added event`() {
        val state = draftState()
        val flag = flag("new_checkout")
        val event = FlagAdded(eventId = eventId(), draftId = state.draftId, flag = flag)

        val actual = RuleManagementEventApplier.apply(state = state, event = event)

        val updated = (actual as Outcome.Ok).value
        assertEquals(mapOf(flag.key to flag), updated.flags)
        assertEquals(RuleSetDraftStatus.Draft, updated.status)
    }

    @Test
    fun `applies flag enabled event`() {
        val flag = flag("new_checkout", enabled = false)
        val state = draftState(flags = mapOf(flag.key to flag))
        val event = FlagEnabled(eventId = eventId(), draftId = state.draftId, flagKey = flag.key)

        val actual = RuleManagementEventApplier.apply(state = state, event = event)

        val updated = (actual as Outcome.Ok).value
        assertEquals(true, updated.flags.getValue(flag.key).enabled)
        assertEquals(RuleSetDraftStatus.Draft, updated.status)
    }

    @Test
    fun `applies flag disabled event`() {
        val flag = flag("new_checkout", enabled = true)
        val state = draftState(flags = mapOf(flag.key to flag))
        val event = FlagDisabled(eventId = eventId(), draftId = state.draftId, flagKey = flag.key)

        val actual = RuleManagementEventApplier.apply(state = state, event = event)

        val updated = (actual as Outcome.Ok).value
        assertEquals(false, updated.flags.getValue(flag.key).enabled)
        assertEquals(RuleSetDraftStatus.Draft, updated.status)
    }

    @Test
    fun `applies rule added event`() {
        val flag = flag("new_checkout")
        val state = draftState(flags = mapOf(flag.key to flag))
        val rule = rule()
        val event = RuleAdded(eventId = eventId(), draftId = state.draftId, flagKey = flag.key, rule = rule)

        val actual = RuleManagementEventApplier.apply(state = state, event = event)

        val updated = (actual as Outcome.Ok).value
        assertEquals(listOf(rule), updated.flags.getValue(flag.key).rules)
        assertEquals(RuleSetDraftStatus.Draft, updated.status)
    }

    @Test
    fun `applies rule changed events`() {
        val rule = rule()
        val flag = flag("new_checkout", rules = listOf(rule))
        val state = draftState(flags = mapOf(flag.key to flag))
        val serve = RuleValue.boolean(true)
        val event =
            RuleServeValueChanged(
                eventId = eventId(),
                draftId = state.draftId,
                flagKey = flag.key,
                ruleId = rule.id,
                serve = serve,
            )

        val actual = RuleManagementEventApplier.apply(state = state, event = event)

        val updated = (actual as Outcome.Ok).value
        assertEquals(
            serve,
            updated
                .flags
                .getValue(flag.key)
                .rules
                .single()
                .serve,
        )
    }

    @Test
    fun `applies rule condition changed event`() {
        val rule = rule()
        val flag = flag("new_checkout", rules = listOf(rule))
        val state = draftState(flags = mapOf(flag.key to flag))
        val condition =
            Condition.Predicate(
                attributeKey = attributeKey("tier"),
                operator = Operator.Eq,
                value = RuleValue.string("pro"),
            )
        val event =
            RuleConditionChanged(
                eventId = eventId(),
                draftId = state.draftId,
                flagKey = flag.key,
                ruleId = rule.id,
                condition = condition,
            )

        val actual = RuleManagementEventApplier.apply(state = state, event = event)

        val updated = (actual as Outcome.Ok).value
        assertEquals(
            condition,
            updated
                .flags
                .getValue(flag.key)
                .rules
                .single()
                .condition,
        )
    }

    @Test
    fun `applies draft validated event`() {
        val state = draftState()
        val event =
            RuleSetDraftValidated(
                eventId = eventId(),
                draftId = state.draftId,
                ruleSetVersion = state.ruleSetVersion,
            )

        val actual = RuleManagementEventApplier.apply(state = state, event = event)

        assertEquals(
            RuleSetDraftStatus.Validated,
            (actual as Outcome.Ok)
                .value
                .status,
        )
    }

    @Test
    fun `rejects event without draft state`() {
        val event =
            FlagAdded(
                eventId = eventId(),
                draftId = draftId(),
                flag = flag("new_checkout"),
            )

        val actual = RuleManagementEventApplier.apply(state = null, event = event)

        assertEquals(Outcome.Err(RuleManagementEventApplyError.DraftNotFound(event.draftId)), actual)
    }

    private fun draftState(flags: Map<FlagKey, Flag> = emptyMap()): RuleSetDraftState =
        RuleSetDraftState(
            draftId = draftId(),
            ruleSetVersion = version(1),
            flags = flags,
            status = RuleSetDraftStatus.Draft,
        )

    private fun flag(
        key: String,
        enabled: Boolean = true,
        rules: List<Rule> = emptyList(),
    ): Flag =
        Flag(
            key = flagKey(key),
            enabled = enabled,
            defaultValue = RuleValue.boolean(false),
            rules = rules,
        )

    private fun rule(): Rule =
        Rule(
            id = ruleId(),
            condition =
                Condition.Predicate(
                    attributeKey = attributeKey("country"),
                    operator = Operator.Eq,
                    value = RuleValue.string("KR"),
                ),
            serve = RuleValue.boolean(false),
        )

    private fun draftId(): RuleSetDraftId =
        when (val draftId = RuleSetDraftId.of(Uuid.parse("018ff7c1-9354-7b02-b021-76d2791d6a21"))) {
            is Outcome.Err -> error("Invalid test draft id: ${draftId.error}")
            is Outcome.Ok -> draftId.value
        }

    private fun eventId(): RuleManagementEventId =
        when (val eventId = RuleManagementEventId.of(Uuid.parse("018ff7c1-9354-7b02-b021-76d2791d6a22"))) {
            is Outcome.Err -> error("Invalid test event id: ${eventId.error}")
            is Outcome.Ok -> eventId.value
        }

    private fun version(value: Long): RuleSetVersion =
        when (val version = RuleSetVersion.of(value)) {
            is Outcome.Err -> error("Invalid test version: ${version.error}")
            is Outcome.Ok -> version.value
        }

    private fun flagKey(value: String): FlagKey =
        when (val key = FlagKey.of(value)) {
            is Outcome.Err -> error("Invalid test flag key: ${key.error}")
            is Outcome.Ok -> key.value
        }

    private fun attributeKey(value: String): AttributeKey =
        when (val key = AttributeKey.of(value)) {
            is Outcome.Err -> error("Invalid test attribute key: ${key.error}")
            is Outcome.Ok -> key.value
        }

    private fun ruleId(): RuleId =
        when (val ruleId = RuleId.of(Uuid.parse("018ff7c1-9354-7b02-b021-76d2791d6a23"))) {
            is Outcome.Err -> error("Invalid test rule id: ${ruleId.error}")
            is Outcome.Ok -> ruleId.value
        }
}
