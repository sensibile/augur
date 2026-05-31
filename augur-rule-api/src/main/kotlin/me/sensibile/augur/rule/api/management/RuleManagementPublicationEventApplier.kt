package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.Outcome

internal object RuleManagementPublicationEventApplier {
    fun applyPublished(
        state: RuleSetDraftState?,
        event: RuleSetPublished,
    ): Outcome<RuleManagementEventApplyError, RuleSetDraftState> = state.updateStatus(event, RuleSetDraftStatus.Published)

    fun applyArchived(
        state: RuleSetDraftState?,
        event: RuleSetArchived,
    ): Outcome<RuleManagementEventApplyError, RuleSetDraftState> = state.updateStatus(event, RuleSetDraftStatus.Archived)
}
