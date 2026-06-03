package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.management.RuleManagementCommandError
import me.sensibile.kopringbricks.web.problem.autoconfigure.ApiException

internal fun RuleManagementCommandError.RuleAlreadyExists.toApiException(): ApiException =
    conflict(
        "rule_already_exists",
        "Rule $ruleId already exists under flag ${existingFlagKey.value} in draft $draftId.",
    )

internal fun RuleManagementCommandError.RuleNotFound.toApiException(): ApiException =
    notFound(
        "rule_not_found",
        "Rule $ruleId was not found under flag ${flagKey.value} in draft $draftId.",
    )

internal fun RuleManagementCommandError.ServeTypeMismatch.toApiException(): ApiException =
    unprocessable(
        "serve_type_mismatch",
        "Rule $ruleId serve type was $actual but flag ${flagKey.value} expects $expected.",
    )
