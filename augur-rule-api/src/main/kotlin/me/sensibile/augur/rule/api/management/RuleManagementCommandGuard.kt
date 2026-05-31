package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.FlagKey
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.RuleId
import me.sensibile.augur.rule.flatMap

internal fun RuleSetDraftState?.requireDraft(draftId: RuleSetDraftId): Outcome<RuleManagementCommandError, RuleSetDraftState> =
    when {
        this == null -> Outcome.Err(RuleManagementCommandError.DraftNotFound(draftId))
        this.draftId != draftId -> Outcome.Err(RuleManagementCommandError.DraftIdMismatch(expected = this.draftId, actual = draftId))
        else -> Outcome.Ok(this)
    }

internal fun RuleSetDraftState?.requireEditableFlag(
    draftId: RuleSetDraftId,
    flagKey: FlagKey,
): Outcome<RuleManagementCommandError, RuleSetDraftState> =
    requireDraft(draftId)
        .flatMap { draft ->
            when {
                draft.status != RuleSetDraftStatus.Draft -> {
                    Outcome.Err(RuleManagementCommandError.DraftIsNotEditable(draftId, draft.status))
                }

                flagKey !in draft.flags -> {
                    Outcome.Err(RuleManagementCommandError.FlagNotFound(draftId, flagKey))
                }

                else -> {
                    Outcome.Ok(draft)
                }
            }
        }

internal fun RuleSetDraftState?.requireEditableRule(
    draftId: RuleSetDraftId,
    flagKey: FlagKey,
    ruleId: RuleId,
): Outcome<RuleManagementCommandError, RuleSetDraftState> =
    requireEditableFlag(draftId, flagKey)
        .flatMap { draft ->
            val ruleExists =
                draft.flags
                    .getValue(flagKey)
                    .rules
                    .any { rule -> rule.id == ruleId }
            if (ruleExists) {
                Outcome.Ok(draft)
            } else {
                Outcome.Err(RuleManagementCommandError.RuleNotFound(draftId, flagKey, ruleId))
            }
        }

internal fun RuleSetDraftState.findRuleFlagKey(ruleId: RuleId): FlagKey? =
    flags.values
        .firstOrNull { flag ->
            flag.rules.any { rule -> rule.id == ruleId }
        }?.key
