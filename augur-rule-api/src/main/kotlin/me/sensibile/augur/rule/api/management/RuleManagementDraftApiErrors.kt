package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.management.RuleManagementCommandError
import me.sensibile.kopringbricks.web.problem.autoconfigure.ApiException

internal fun RuleManagementCommandError.DraftAlreadyExists.toApiException(): ApiException =
    conflict("draft_already_exists", "Rule set draft $draftId already exists.")

internal fun RuleManagementCommandError.DraftNotFound.toApiException(): ApiException = DraftNotFoundException(draftId)

internal fun RuleManagementCommandError.DraftIdMismatch.toApiException(): ApiException =
    conflict(
        "draft_id_mismatch",
        "Expected draft $expected but command targeted $actual.",
    )

internal fun RuleManagementCommandError.DraftIsNotEditable.toApiException(): ApiException =
    conflict(
        "draft_is_not_editable",
        "Rule set draft $draftId is $status and cannot be edited.",
    )

internal fun RuleManagementCommandError.DraftIsNotPublishable.toApiException(): ApiException =
    conflict(
        "draft_is_not_publishable",
        "Rule set draft $draftId is $status and cannot be published.",
    )

internal fun RuleManagementCommandError.DraftIsNotArchivable.toApiException(): ApiException =
    conflict(
        "draft_is_not_archivable",
        "Rule set draft $draftId is $status and cannot be archived.",
    )

internal fun RuleManagementCommandError.InvalidRuleSetDraft.toApiException(): ApiException =
    unprocessable("invalid_rule_set_draft", "Rule set draft $draftId failed validation.")
