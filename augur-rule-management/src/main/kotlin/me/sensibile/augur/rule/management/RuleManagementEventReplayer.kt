package me.sensibile.augur.rule.management

import me.sensibile.augur.rule.Outcome

object RuleManagementEventReplayer {
    fun replay(events: Sequence<RuleManagementEvent>): Outcome<RuleManagementEventApplyError, RuleSetDraftState?> =
        replay(events.asIterable())

    fun replay(events: Iterable<RuleManagementEvent>): Outcome<RuleManagementEventApplyError, RuleSetDraftState?> {
        var state: RuleSetDraftState? = null
        for (event in events) {
            when (val applied = RuleManagementEventApplier.apply(state = state, event = event)) {
                is Outcome.Err -> return applied
                is Outcome.Ok -> state = applied.value
            }
        }
        return Outcome.Ok(state)
    }
}
