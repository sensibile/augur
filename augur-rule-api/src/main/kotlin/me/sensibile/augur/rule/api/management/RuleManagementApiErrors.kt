package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.ValueObjectError
import me.sensibile.augur.rule.management.RuleManagementCommandError
import me.sensibile.augur.rule.management.RuleManagementCommandServiceError
import me.sensibile.augur.rule.management.RuleManagementEventApplyError
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
        is RuleManagementCommandError.DraftAlreadyExists -> toApiException()
        is RuleManagementCommandError.DraftNotFound -> toApiException()
        is RuleManagementCommandError.DraftIdMismatch -> toApiException()
        is RuleManagementCommandError.DraftIsNotEditable -> toApiException()
        is RuleManagementCommandError.DraftIsNotPublishable -> toApiException()
        is RuleManagementCommandError.DraftIsNotArchivable -> toApiException()
        is RuleManagementCommandError.InvalidRuleSetDraft -> toApiException()
        is RuleManagementCommandError.FlagAlreadyExists -> toApiException()
        is RuleManagementCommandError.FlagNotFound -> toApiException()
        is RuleManagementCommandError.RuleAlreadyExists -> toApiException()
        is RuleManagementCommandError.RuleNotFound -> toApiException()
        is RuleManagementCommandError.ServeTypeMismatch -> toApiException()
    }

private fun RuleManagementEventApplyError.toApiException(): ApiException =
    conflict(
        code = "rule_management_event_replay_failed",
        detail = "Stored rule management events could not be replayed.",
    )

internal class DraftNotFoundException(
    draftId: RuleSetDraftId,
) : ApiException(
        status = HttpStatus.NOT_FOUND,
        code = "draft_not_found",
        detail = "Rule set draft $draftId was not found.",
        title = "Rule management resource not found",
    )

internal fun conflict(
    code: String,
    detail: String,
): ApiException =
    ApiException(
        status = HttpStatus.CONFLICT,
        code = code,
        detail = detail,
        title = "Rule management command rejected",
    )

internal fun notFound(
    code: String,
    detail: String,
): ApiException =
    ApiException(
        status = HttpStatus.NOT_FOUND,
        code = code,
        detail = detail,
        title = "Rule management resource not found",
    )

internal fun unprocessable(
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
        code =
            when (error.field) {
                "flagKey" -> "invalid_flag_key"
                "ruleSetVersion" -> "invalid_rule_set_version"
                "ruleManagementStreamVersion" -> "invalid_rule_management_stream_version"
                "ruleSetDraftId" -> "invalid_rule_set_draft_id"
                else -> "invalid_${error.field}"
            },
        detail = "Invalid value for ${error.field}.",
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
