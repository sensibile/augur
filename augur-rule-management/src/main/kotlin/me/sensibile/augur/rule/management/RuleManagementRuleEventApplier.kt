package me.sensibile.augur.rule.management

import me.sensibile.augur.rule.Outcome

internal object RuleManagementRuleEventApplier {
    fun applyAdded(
        state: RuleSetDraftState?,
        event: RuleAdded,
    ): Outcome<RuleManagementEventApplyError, RuleSetDraftState> =
        state.updateFlag(event, event.flagKey) { flag ->
            flag.copy(rules = flag.rules + event.rule)
        }

    fun applyConditionChanged(
        state: RuleSetDraftState?,
        event: RuleConditionChanged,
    ): Outcome<RuleManagementEventApplyError, RuleSetDraftState> =
        state.updateRule(event, event.flagKey, event.ruleId) { rule ->
            rule.copy(condition = event.condition)
        }

    fun applyServeValueChanged(
        state: RuleSetDraftState?,
        event: RuleServeValueChanged,
    ): Outcome<RuleManagementEventApplyError, RuleSetDraftState> =
        state.updateRule(event, event.flagKey, event.ruleId) { rule ->
            rule.copy(serve = event.serve)
        }

    fun applyRemoved(
        state: RuleSetDraftState?,
        event: RuleRemoved,
    ): Outcome<RuleManagementEventApplyError, RuleSetDraftState> = state.removeRule(event, event.flagKey, event.ruleId)
}
