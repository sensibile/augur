package me.sensibile.augur.rule.management

import me.sensibile.augur.rule.Outcome

internal object RuleManagementDraftEventApplier {
    fun applyCreated(
        state: RuleSetDraftState?,
        event: RuleSetDraftCreated,
    ): Outcome<RuleManagementEventApplyError, RuleSetDraftState> =
        if (state == null) {
            Outcome.Ok(
                RuleSetDraftState(
                    draftId = event.draftId,
                    ruleSetVersion = event.ruleSetVersion,
                    flags = emptyMap(),
                    status = RuleSetDraftStatus.Draft,
                ),
            )
        } else {
            Outcome.Err(RuleManagementEventApplyError.DraftAlreadyExists(event.draftId))
        }

    fun applyValidated(
        state: RuleSetDraftState?,
        event: RuleSetDraftValidated,
    ): Outcome<RuleManagementEventApplyError, RuleSetDraftState> = state.updateStatus(event, RuleSetDraftStatus.Validated)
}
