package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.Flag
import me.sensibile.augur.rule.FlagKey
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.Rule
import me.sensibile.augur.rule.RuleId
import me.sensibile.augur.rule.flatMap
import me.sensibile.augur.rule.map

internal fun RuleSetDraftState?.updateStatus(
    event: RuleManagementEvent,
    status: RuleSetDraftStatus,
): Outcome<RuleManagementEventApplyError, RuleSetDraftState> =
    requireState(event).map { draft ->
        draft.copy(status = status)
    }

internal fun RuleSetDraftState?.updateRule(
    event: RuleManagementEvent,
    flagKey: FlagKey,
    ruleId: RuleId,
    transform: (Rule) -> Rule,
): Outcome<RuleManagementEventApplyError, RuleSetDraftState> =
    updateFlag(event, flagKey) { flag ->
        flag.copy(
            rules =
                flag.rules.map { rule ->
                    if (rule.id == ruleId) {
                        transform(rule)
                    } else {
                        rule
                    }
                },
        )
    }.flatMap { draft ->
        if (draft.containsRule(flagKey, ruleId)) {
            Outcome.Ok(draft)
        } else {
            Outcome.Err(RuleManagementEventApplyError.RuleNotFound(event.draftId, flagKey, ruleId))
        }
    }

internal fun RuleSetDraftState?.removeRule(
    event: RuleManagementEvent,
    flagKey: FlagKey,
    ruleId: RuleId,
): Outcome<RuleManagementEventApplyError, RuleSetDraftState> =
    requireState(event).flatMap { draft ->
        val flag = draft.flags[flagKey]
        when {
            flag == null -> {
                Outcome.Err(RuleManagementEventApplyError.FlagNotFound(event.draftId, flagKey))
            }

            flag.rules.none { rule -> rule.id == ruleId } -> {
                Outcome.Err(RuleManagementEventApplyError.RuleNotFound(event.draftId, flagKey, ruleId))
            }

            else -> {
                val updatedFlag =
                    flag.copy(
                        rules = flag.rules.filterNot { rule -> rule.id == ruleId },
                    )
                Outcome.Ok(
                    draft.copy(
                        flags = draft.flags + (flagKey to updatedFlag),
                        status = RuleSetDraftStatus.Draft,
                    ),
                )
            }
        }
    }

internal fun RuleSetDraftState?.updateFlag(
    event: RuleManagementEvent,
    flagKey: FlagKey,
    transform: (Flag) -> Flag,
): Outcome<RuleManagementEventApplyError, RuleSetDraftState> =
    requireState(event).flatMap { draft ->
        val flag = draft.flags[flagKey]
        if (flag == null) {
            Outcome.Err(RuleManagementEventApplyError.FlagNotFound(event.draftId, flagKey))
        } else {
            Outcome.Ok(
                draft.copy(
                    flags = draft.flags + (flagKey to transform(flag)),
                    status = RuleSetDraftStatus.Draft,
                ),
            )
        }
    }

internal fun RuleSetDraftState?.requireState(event: RuleManagementEvent): Outcome<RuleManagementEventApplyError, RuleSetDraftState> =
    when {
        this == null -> {
            Outcome.Err(RuleManagementEventApplyError.DraftNotFound(event.draftId))
        }

        draftId != event.draftId -> {
            Outcome.Err(
                RuleManagementEventApplyError.DraftIdMismatch(
                    expected = draftId,
                    actual = event.draftId,
                ),
            )
        }

        else -> {
            Outcome.Ok(this)
        }
    }

private fun RuleSetDraftState.containsRule(
    flagKey: FlagKey,
    ruleId: RuleId,
): Boolean = flags.getValue(flagKey).rules.any { rule -> rule.id == ruleId }
