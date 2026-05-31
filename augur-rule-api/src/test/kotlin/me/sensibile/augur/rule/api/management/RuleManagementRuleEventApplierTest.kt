package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.Operator
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.RuleValue
import kotlin.test.Test
import kotlin.test.assertEquals

class RuleManagementRuleEventApplierTest {
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
}
