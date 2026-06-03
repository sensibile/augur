package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.ValueObjectError
import me.sensibile.augur.rule.management.RuleManagementCommandError
import me.sensibile.augur.rule.management.RuleManagementCommandServiceError
import me.sensibile.augur.rule.management.RuleManagementEventApplyError
import me.sensibile.augur.rule.management.RuleManagementEventStoreError
import me.sensibile.augur.rule.management.RuleManagementProcessError
import me.sensibile.augur.rule.management.RuleSetDraftId
import me.sensibile.kopringbricks.web.problem.autoconfigure.ApiException
import org.springframework.http.HttpStatus

internal fun RuleManagementCommandServiceError.toApiException(): ApiException =
    when (this) {
        is RuleManagementCommandServiceError.EventStoreLoadFailed -> error.toApiException()
        is RuleManagementCommandServiceError.EventReplayFailed -> error.toApiException()
        is RuleManagementCommandServiceError.CommandProcessFailed -> error.toApiException()
        is RuleManagementCommandServiceError.EventStoreAppendFailed -> error.toApiException()
    }

private fun RuleManagementProcessError.toApiException(): ApiException =
    when (this) {
        is RuleManagementProcessError.CommandRejected -> error.toApiException()
        is RuleManagementProcessError.EventApplyFailed -> error.toApiException()
    }

private fun RuleManagementCommandError.toApiException(): ApiException =
    when (this) {
        is RuleManagementCommandError.DraftAlreadyExists,
        is RuleManagementCommandError.DraftNotFound,
        is RuleManagementCommandError.DraftIdMismatch,
        is RuleManagementCommandError.DraftIsNotEditable,
        is RuleManagementCommandError.DraftIsNotPublishable,
        is RuleManagementCommandError.DraftIsNotArchivable,
        -> {
            toDraftApiException()
        }

        is RuleManagementCommandError.FlagAlreadyExists,
        is RuleManagementCommandError.FlagNotFound,
        -> {
            toFlagApiException()
        }

        is RuleManagementCommandError.RuleAlreadyExists,
        is RuleManagementCommandError.RuleNotFound,
        is RuleManagementCommandError.ServeTypeMismatch,
        -> {
            toRuleApiException()
        }

        is RuleManagementCommandError.InvalidRuleSetDraft -> {
            unprocessable("invalid_rule_set_draft", "Rule set draft validation failed.")
        }
    }

private fun RuleManagementCommandError.toDraftApiException(): ApiException =
    when (this) {
        is RuleManagementCommandError.DraftAlreadyExists -> {
            conflict("draft_already_exists", "Rule set draft $draftId already exists.")
        }

        is RuleManagementCommandError.DraftNotFound -> {
            DraftNotFoundException(draftId)
        }

        is RuleManagementCommandError.DraftIdMismatch -> {
            conflict(
                "draft_id_mismatch",
                "Expected draft $expected but command targeted $actual.",
            )
        }

        is RuleManagementCommandError.DraftIsNotEditable -> {
            conflict(
                "draft_is_not_editable",
                "Rule set draft $draftId is $status and cannot be edited.",
            )
        }

        is RuleManagementCommandError.DraftIsNotPublishable -> {
            conflict(
                "draft_is_not_publishable",
                "Rule set draft $draftId is $status and cannot be published.",
            )
        }

        is RuleManagementCommandError.DraftIsNotArchivable -> {
            conflict(
                "draft_is_not_archivable",
                "Rule set draft $draftId is $status and cannot be archived.",
            )
        }

        else -> {
            error("Not a draft command error: $this")
        }
    }

private fun RuleManagementCommandError.toFlagApiException(): ApiException =
    when (this) {
        is RuleManagementCommandError.FlagAlreadyExists -> {
            conflict(
                "flag_already_exists",
                "Flag ${flagKey.value} already exists in draft $draftId.",
            )
        }

        is RuleManagementCommandError.FlagNotFound -> {
            notFound("flag_not_found", "Flag ${flagKey.value} was not found in draft $draftId.")
        }

        else -> {
            error("Not a flag command error: $this")
        }
    }

private fun RuleManagementCommandError.toRuleApiException(): ApiException =
    when (this) {
        is RuleManagementCommandError.RuleAlreadyExists -> {
            conflict(
                "rule_already_exists",
                "Rule $ruleId already exists under flag ${existingFlagKey.value} in draft $draftId.",
            )
        }

        is RuleManagementCommandError.RuleNotFound -> {
            notFound(
                "rule_not_found",
                "Rule $ruleId was not found under flag ${flagKey.value} in draft $draftId.",
            )
        }

        is RuleManagementCommandError.ServeTypeMismatch -> {
            unprocessable(
                "serve_type_mismatch",
                "Rule $ruleId serve type was $actual but flag ${flagKey.value} expects $expected.",
            )
        }

        else -> {
            error("Not a rule command error: $this")
        }
    }

private fun RuleManagementEventApplyError.toApiException(): ApiException =
    conflict(
        code = "rule_management_event_replay_failed",
        detail = toString(),
    )

internal fun RuleManagementEventStoreError.toApiException(): ApiException =
    when (this) {
        is RuleManagementEventStoreError.StreamVersionConflict -> {
            conflict(
                code = "stream_version_conflict",
                detail = "Expected stream version $expected but actual version was $actual.",
            )
        }

        is RuleManagementEventStoreError.StorageFailure -> {
            ApiException(
                status = HttpStatus.INTERNAL_SERVER_ERROR,
                code = "rule_management_storage_failure",
                detail = message,
                title = "Rule management storage failed",
            )
        }
    }

internal class DraftNotFoundException(
    draftId: RuleSetDraftId,
) : ApiException(
        status = HttpStatus.NOT_FOUND,
        code = "draft_not_found",
        detail = "Rule set draft $draftId was not found.",
        title = "Rule management resource not found",
    )

private fun conflict(
    code: String,
    detail: String,
): ApiException =
    ApiException(
        status = HttpStatus.CONFLICT,
        code = code,
        detail = detail,
        title = "Rule management command rejected",
    )

private fun notFound(
    code: String,
    detail: String,
): ApiException =
    ApiException(
        status = HttpStatus.NOT_FOUND,
        code = code,
        detail = detail,
        title = "Rule management resource not found",
    )

private fun unprocessable(
    code: String,
    detail: String,
): ApiException =
    ApiException(
        status = HttpStatus.UNPROCESSABLE_ENTITY,
        code = code,
        detail = detail,
        title = "Rule management command rejected",
    )

internal class RuleJsonException(
    error: me.sensibile.augur.rule.api.RuleSetValidationErrorResponse,
) : ApiException(
        status = HttpStatus.UNPROCESSABLE_ENTITY,
        code = error.code,
        detail = error.message,
        title = "Rule JSON is invalid",
        properties =
            if (error.violations.isEmpty()) {
                emptyMap()
            } else {
                mapOf("violations" to error.violations)
            },
    )

internal class ValueObjectException(
    error: ValueObjectError,
) : BadRequestException(
        code = "invalid_${error.field}",
        detail = error.toString(),
    )

internal open class BadRequestException(
    code: String,
    detail: String,
    cause: Throwable? = null,
) : ApiException(
        status = HttpStatus.BAD_REQUEST,
        code = code,
        detail = detail,
        title = "Rule management request is invalid",
        cause = cause,
    )
