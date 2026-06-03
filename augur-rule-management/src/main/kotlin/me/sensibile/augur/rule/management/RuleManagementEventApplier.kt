package me.sensibile.augur.rule.management

import me.sensibile.augur.rule.FlagKey
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.RuleId

object RuleManagementEventApplier {
    fun apply(
        state: RuleSetDraftState?,
        event: RuleManagementEvent,
    ): Outcome<RuleManagementEventApplyError, RuleSetDraftState> =
        when (event) {
            is RuleSetDraftCreated -> {
                RuleManagementDraftEventApplier.applyCreated(state, event)
            }

            is FlagAdded -> {
                RuleManagementFlagEventApplier.applyAdded(state, event)
            }

            is FlagEnabled -> {
                RuleManagementFlagEventApplier.applyEnabled(state, event)
            }

            is FlagDisabled -> {
                RuleManagementFlagEventApplier.applyDisabled(state, event)
            }

            is RuleAdded -> {
                RuleManagementRuleEventApplier.applyAdded(state, event)
            }

            is RuleConditionChanged -> {
                RuleManagementRuleEventApplier.applyConditionChanged(state, event)
            }

            is RuleServeValueChanged -> {
                RuleManagementRuleEventApplier.applyServeValueChanged(state, event)
            }

            is RuleRemoved -> {
                RuleManagementRuleEventApplier.applyRemoved(state, event)
            }

            is RuleSetDraftValidated -> {
                RuleManagementDraftEventApplier.applyValidated(state, event)
            }

            is RuleSetPublished -> {
                RuleManagementPublicationEventApplier.applyPublished(state, event)
            }

            is RuleSetArchived -> {
                RuleManagementPublicationEventApplier.applyArchived(state, event)
            }
        }
}

sealed interface RuleManagementEventApplyError {
    data class DraftAlreadyExists(
        val draftId: RuleSetDraftId,
    ) : RuleManagementEventApplyError

    data class DraftNotFound(
        val draftId: RuleSetDraftId,
    ) : RuleManagementEventApplyError

    data class DraftIdMismatch(
        val expected: RuleSetDraftId,
        val actual: RuleSetDraftId,
    ) : RuleManagementEventApplyError

    data class FlagNotFound(
        val draftId: RuleSetDraftId,
        val flagKey: FlagKey,
    ) : RuleManagementEventApplyError

    data class RuleNotFound(
        val draftId: RuleSetDraftId,
        val flagKey: FlagKey,
        val ruleId: RuleId,
    ) : RuleManagementEventApplyError
}
