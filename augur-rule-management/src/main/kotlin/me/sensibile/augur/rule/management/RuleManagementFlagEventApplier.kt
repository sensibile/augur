package me.sensibile.augur.rule.management

import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.map

internal object RuleManagementFlagEventApplier {
    fun applyAdded(
        state: RuleSetDraftState?,
        event: FlagAdded,
    ): Outcome<RuleManagementEventApplyError, RuleSetDraftState> =
        state
            .requireState(event)
            .map { draft ->
                draft.copy(
                    flags = draft.flags + (event.flag.key to event.flag),
                    status = RuleSetDraftStatus.Draft,
                )
            }

    fun applyEnabled(
        state: RuleSetDraftState?,
        event: FlagEnabled,
    ): Outcome<RuleManagementEventApplyError, RuleSetDraftState> =
        state.updateFlag(event, event.flagKey) { flag ->
            flag.copy(enabled = true)
        }

    fun applyDisabled(
        state: RuleSetDraftState?,
        event: FlagDisabled,
    ): Outcome<RuleManagementEventApplyError, RuleSetDraftState> =
        state.updateFlag(event, event.flagKey) { flag ->
            flag.copy(enabled = false)
        }
}
