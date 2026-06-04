@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package me.sensibile.augur.rule.api

import me.sensibile.augur.rule.AttributeKey
import me.sensibile.augur.rule.BranchKind
import me.sensibile.augur.rule.FlagKey
import me.sensibile.augur.rule.Operator
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.RuleId
import me.sensibile.augur.rule.RuleSetValidationError
import me.sensibile.augur.rule.RuleSetViolation
import me.sensibile.augur.rule.RuleValueType
import me.sensibile.augur.rule.ValueObjectError
import me.sensibile.augur.rule.json.RuleJsonError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class RuleSetValidationMapperTest {
    @Test
    fun `maps non validation json errors to response codes`() {
        assertEquals(
            RuleSetValidationErrorResponse("invalid_json", "Rule set JSON is invalid."),
            RuleJsonError.InvalidJson("").toValidationError(),
        )
        assertEquals(
            RuleSetValidationErrorResponse("invalid_value_object", ValueObjectError.Blank("flagKey").toString()),
            RuleJsonError.InvalidValueObject(ValueObjectError.Blank("flagKey")).toValidationError(),
        )
        assertEquals(
            RuleSetValidationErrorResponse("invalid_rule_value", "Unsupported rule value type: List"),
            RuleJsonError.InvalidRuleValue(RuleValueType.List).toValidationError(),
        )
        assertEquals(
            RuleSetValidationErrorResponse("duplicate_flag_key", "Duplicate flag key: new_checkout"),
            RuleJsonError.DuplicateFlagKey(flagKey("new_checkout")).toValidationError(),
        )
    }

    @Test
    fun `maps every rule set violation to response violations`() {
        val ruleId = ruleId()
        val response =
            RuleJsonError
                .InvalidRuleSet(
                    RuleSetValidationError(
                        listOf(
                            RuleSetViolation.FlagKeyMismatch(flagKey("map_key"), flagKey("flag_key")),
                            RuleSetViolation.DuplicateRuleId(ruleId, listOf(flagKey("one"), flagKey("two"))),
                            RuleSetViolation.ServeTypeMismatch(
                                flagKey = flagKey("new_checkout"),
                                ruleId = ruleId,
                                expected = RuleValueType.Boolean,
                                actual = RuleValueType.String,
                            ),
                            RuleSetViolation.EmptyConditionBranch(
                                flagKey = flagKey("new_checkout"),
                                ruleId = ruleId,
                                branchKind = BranchKind.All,
                            ),
                            RuleSetViolation.InvalidPredicateValue(
                                flagKey = flagKey("new_checkout"),
                                ruleId = ruleId,
                                attributeKey = attributeKey("country"),
                                operator = Operator.In,
                                expectedTypes = setOf(RuleValueType.List),
                                actual = RuleValueType.String,
                            ),
                        ),
                    ),
                ).toValidationError()

        assertEquals("invalid_rule_set", response.code)
        assertEquals(
            listOf(
                "flag_key_mismatch",
                "duplicate_rule_id",
                "serve_type_mismatch",
                "empty_condition_branch",
                "invalid_predicate_value",
            ),
            response.violations.map(RuleSetViolationResponse::code),
        )
    }

    private fun flagKey(value: String): FlagKey =
        when (val result = FlagKey.of(value)) {
            is Outcome.Err -> error("Invalid test flag key: $value")
            is Outcome.Ok -> result.value
        }

    private fun attributeKey(value: String): AttributeKey =
        when (val result = AttributeKey.of(value)) {
            is Outcome.Err -> error("Invalid test attribute key: $value")
            is Outcome.Ok -> result.value
        }

    private fun ruleId(): RuleId =
        when (val result = RuleId.of(Uuid.parse("018ff7c1-9354-7b02-b021-76d2791d6a21"))) {
            is Outcome.Err -> error("Invalid test rule id: ${result.error}")
            is Outcome.Ok -> result.value
        }
}
