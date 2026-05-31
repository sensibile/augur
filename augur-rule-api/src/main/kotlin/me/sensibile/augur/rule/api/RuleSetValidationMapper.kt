package me.sensibile.augur.rule.api

import me.sensibile.augur.rule.RuleSetSnapshot
import me.sensibile.augur.rule.RuleSetViolation
import me.sensibile.augur.rule.json.RuleJsonError

internal fun RuleSetSnapshot.toSummary(): RuleSetSnapshotSummary =
    RuleSetSnapshotSummary(
        version = value.version.value,
        flagCount = value.flags.size,
    )

internal fun RuleJsonError.toValidationError(): RuleSetValidationErrorResponse =
    when (this) {
        is RuleJsonError.InvalidJson -> {
            RuleSetValidationErrorResponse(
                code = "invalid_json",
                message = message.ifBlank { "Rule set JSON is invalid." },
            )
        }

        is RuleJsonError.InvalidValueObject -> {
            RuleSetValidationErrorResponse(
                code = "invalid_value_object",
                message = error.toString(),
            )
        }

        is RuleJsonError.InvalidRuleValue -> {
            RuleSetValidationErrorResponse(
                code = "invalid_rule_value",
                message = "Unsupported rule value type: $type",
            )
        }

        is RuleJsonError.InvalidRuleSet -> {
            RuleSetValidationErrorResponse(
                code = "invalid_rule_set",
                message = "Rule set validation failed.",
                violations = error.violations.map(RuleSetViolation::toViolationResponse),
            )
        }

        is RuleJsonError.DuplicateFlagKey -> {
            RuleSetValidationErrorResponse(
                code = "duplicate_flag_key",
                message = "Duplicate flag key: ${key.value}",
            )
        }
    }

private fun RuleSetViolation.toViolationResponse(): RuleSetViolationResponse =
    when (this) {
        is RuleSetViolation.FlagKeyMismatch -> {
            RuleSetViolationResponse(
                code = "flag_key_mismatch",
                message = "Flag map key ${mapKey.value} does not match flag key ${flagKey.value}.",
            )
        }

        is RuleSetViolation.DuplicateRuleId -> {
            RuleSetViolationResponse(
                code = "duplicate_rule_id",
                message = "Rule id $ruleId is duplicated across flags ${flagKeys.joinToString { it.value }}.",
            )
        }

        is RuleSetViolation.ServeTypeMismatch -> {
            RuleSetViolationResponse(
                code = "serve_type_mismatch",
                message = "Rule $ruleId for flag ${flagKey.value} serves $actual but default value is $expected.",
            )
        }

        is RuleSetViolation.EmptyConditionBranch -> {
            RuleSetViolationResponse(
                code = "empty_condition_branch",
                message = "Rule $ruleId for flag ${flagKey.value} has an empty $branchKind condition branch.",
            )
        }

        is RuleSetViolation.InvalidPredicateValue -> {
            RuleSetViolationResponse(
                code = "invalid_predicate_value",
                message =
                    "Rule $ruleId for flag ${flagKey.value} uses $operator with $actual for ${attributeKey.value}; " +
                        "expected $expectedTypes.",
            )
        }
    }
