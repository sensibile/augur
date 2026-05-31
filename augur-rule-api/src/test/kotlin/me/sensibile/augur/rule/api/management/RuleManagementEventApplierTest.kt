package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.Operator
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.RuleValue
import kotlin.test.Test
import kotlin.test.assertEquals

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
        val rule = rule(serve = RuleValue.boolean(false))
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
        val condition = condition("tier", Operator.Eq, RuleValue.string("pro"))
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
    fun `applies rule removed event`() {
        val removedRule = rule()
        val remainingRule = rule("018ff7c1-9354-7b02-b021-76d2791d6a24")
        val flag = flag("new_checkout", rules = listOf(removedRule, remainingRule))
        val state = draftState(flags = mapOf(flag.key to flag))
        val event =
            RuleRemoved(
                eventId = eventId(),
                draftId = state.draftId,
                flagKey = flag.key,
                ruleId = removedRule.id,
            )

        val actual = RuleManagementEventApplier.apply(state = state, event = event)

        val updated = (actual as Outcome.Ok).value
        assertEquals(listOf(remainingRule), updated.flags.getValue(flag.key).rules)
        assertEquals(RuleSetDraftStatus.Draft, updated.status)
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
    fun `applies rule set published event`() {
        val state = draftState(status = RuleSetDraftStatus.Validated)
        val event =
            RuleSetPublished(
                eventId = eventId(),
                draftId = state.draftId,
                publishedRuleSetId = publishedRuleSetId(),
                ruleSetVersion = state.ruleSetVersion,
            )

        val actual = RuleManagementEventApplier.apply(state = state, event = event)

        assertEquals(
            RuleSetDraftStatus.Published,
            (actual as Outcome.Ok)
                .value
                .status,
        )
    }

    @Test
    fun `applies rule set archived event`() {
        val state = draftState(status = RuleSetDraftStatus.Published)
        val event =
            RuleSetArchived(
                eventId = eventId(),
                draftId = state.draftId,
            )

        val actual = RuleManagementEventApplier.apply(state = state, event = event)

        assertEquals(
            RuleSetDraftStatus.Archived,
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
}
