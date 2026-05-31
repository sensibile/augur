package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.FlagKey
import me.sensibile.augur.rule.RuleId
import me.sensibile.augur.rule.RuleSetValidationError
import me.sensibile.augur.rule.RuleValueType

sealed interface RuleManagementCommandError {
    data class DraftAlreadyExists(
        val draftId: RuleSetDraftId,
    ) : RuleManagementCommandError

    data class DraftNotFound(
        val draftId: RuleSetDraftId,
    ) : RuleManagementCommandError

    data class DraftIdMismatch(
        val expected: RuleSetDraftId,
        val actual: RuleSetDraftId,
    ) : RuleManagementCommandError

    data class DraftIsNotEditable(
        val draftId: RuleSetDraftId,
        val status: RuleSetDraftStatus,
    ) : RuleManagementCommandError

    data class DraftIsNotPublishable(
        val draftId: RuleSetDraftId,
        val status: RuleSetDraftStatus,
    ) : RuleManagementCommandError

    data class DraftIsNotArchivable(
        val draftId: RuleSetDraftId,
        val status: RuleSetDraftStatus,
    ) : RuleManagementCommandError

    data class FlagAlreadyExists(
        val draftId: RuleSetDraftId,
        val flagKey: FlagKey,
    ) : RuleManagementCommandError

    data class FlagNotFound(
        val draftId: RuleSetDraftId,
        val flagKey: FlagKey,
    ) : RuleManagementCommandError

    data class RuleAlreadyExists(
        val draftId: RuleSetDraftId,
        val ruleId: RuleId,
        val existingFlagKey: FlagKey,
    ) : RuleManagementCommandError

    data class RuleNotFound(
        val draftId: RuleSetDraftId,
        val flagKey: FlagKey,
        val ruleId: RuleId,
    ) : RuleManagementCommandError

    data class ServeTypeMismatch(
        val draftId: RuleSetDraftId,
        val flagKey: FlagKey,
        val ruleId: RuleId,
        val expected: RuleValueType,
        val actual: RuleValueType,
    ) : RuleManagementCommandError

    data class InvalidRuleSetDraft(
        val draftId: RuleSetDraftId,
        val error: RuleSetValidationError,
    ) : RuleManagementCommandError
}
