package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.management.RuleManagementEventStoreError
import me.sensibile.kopringbricks.web.problem.autoconfigure.ApiException
import org.springframework.http.HttpStatus

internal fun RuleManagementEventStoreError.toApiException(): ApiException =
    when (this) {
        is RuleManagementEventStoreError.StreamVersionConflict -> {
            conflict(
                code = "stream_version_conflict",
                detail = "Rule management stream version does not match the expected version.",
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
