package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.management.RuleManagementCommandError
import me.sensibile.kopringbricks.web.problem.autoconfigure.ApiException

internal fun RuleManagementCommandError.FlagAlreadyExists.toApiException(): ApiException =
    conflict(
        "flag_already_exists",
        "Flag ${flagKey.value} already exists in draft $draftId.",
    )

internal fun RuleManagementCommandError.FlagNotFound.toApiException(): ApiException =
    notFound("flag_not_found", "Flag ${flagKey.value} was not found in draft $draftId.")
