package me.sensibile.augur.rule

object RuleSetValidator {
    fun validate(ruleSet: RuleSet): Outcome<RuleSetValidationError, ValidRuleSet> {
        val violations = mutableListOf<RuleSetViolation>()

        violations += validateRuleIds(ruleSet)

        ruleSet.flags.forEach { (flagKey, flag) ->
            if (flagKey != flag.key) {
                violations += RuleSetViolation.FlagKeyMismatch(flagKey, flag.key)
            }

            violations += validateFlag(flag)
        }

        return if (violations.isEmpty()) {
            Outcome.Ok(ValidRuleSet(ruleSet))
        } else {
            Outcome.Err(RuleSetValidationError(violations))
        }
    }

    private fun validateRuleIds(ruleSet: RuleSet): List<RuleSetViolation> =
        ruleSet.flags.values
            .flatMap { flag ->
                flag.rules.map { rule ->
                    RuleReference(flag.key, rule.id)
                }
            }.groupBy(RuleReference::ruleId)
            .filterValues { references -> references.size > 1 }
            .map { (ruleId, references) ->
                RuleSetViolation.DuplicateRuleId(
                    ruleId = ruleId,
                    flagKeys = references.map(RuleReference::flagKey).distinct(),
                )
            }

    private fun validateFlag(flag: Flag): List<RuleSetViolation> =
        validateServeTypes(flag) +
            flag.rules.flatMap { rule ->
                validateCondition(flag.key, rule.id, rule.condition)
            }

    private fun validateServeTypes(flag: Flag): List<RuleSetViolation> =
        flag.rules
            .filter { rule -> rule.serve.type() != flag.defaultValue.type() }
            .map { rule ->
                RuleSetViolation.ServeTypeMismatch(
                    flagKey = flag.key,
                    ruleId = rule.id,
                    expected = flag.defaultValue.type(),
                    actual = rule.serve.type(),
                )
            }

    private data class RuleReference(
        val flagKey: FlagKey,
        val ruleId: RuleId,
    )

    private fun validateCondition(
        flagKey: FlagKey,
        ruleId: RuleId,
        condition: Condition,
    ): List<RuleSetViolation> =
        when (condition) {
            is Condition.All -> validateBranch(flagKey, ruleId, condition.conditions, BranchKind.All)
            is Condition.Any -> validateBranch(flagKey, ruleId, condition.conditions, BranchKind.Any)
            is Condition.Not -> validateCondition(flagKey, ruleId, condition.condition)
            is Condition.Predicate -> validatePredicate(flagKey, ruleId, condition)
        }

    private fun validateBranch(
        flagKey: FlagKey,
        ruleId: RuleId,
        conditions: List<Condition>,
        branchKind: BranchKind,
    ): List<RuleSetViolation> =
        if (conditions.isEmpty()) {
            listOf(RuleSetViolation.EmptyConditionBranch(flagKey, ruleId, branchKind))
        } else {
            conditions.flatMap { condition ->
                validateCondition(flagKey, ruleId, condition)
            }
        }

    private fun validatePredicate(
        flagKey: FlagKey,
        ruleId: RuleId,
        predicate: Condition.Predicate,
    ): List<RuleSetViolation> {
        val expectedType = predicate.operator.expectedValueType()
        return if (expectedType.accepts(predicate.value)) {
            emptyList()
        } else {
            listOf(
                RuleSetViolation.InvalidPredicateValue(
                    flagKey = flagKey,
                    ruleId = ruleId,
                    attributeKey = predicate.attributeKey,
                    operator = predicate.operator,
                    expectedTypes = expectedType.types,
                    actual = predicate.value.type(),
                ),
            )
        }
    }

    private fun Operator.expectedValueType(): RuleValueExpectation =
        when (this) {
            Operator.Eq,
            Operator.NotEq,
            -> RuleValueExpectation.AnyComparable

            Operator.In,
            Operator.NotIn,
            -> RuleValueExpectation.List

            Operator.GreaterThan,
            Operator.GreaterThanOrEqual,
            Operator.LessThan,
            Operator.LessThanOrEqual,
            -> RuleValueExpectation.Number

            Operator.Exists,
            Operator.Missing,
            -> RuleValueExpectation.NoValue

            Operator.Contains -> RuleValueExpectation.ContainsElement

            Operator.StartsWith,
            Operator.EndsWith,
            -> RuleValueExpectation.String
        }
}

@JvmInline
value class ValidRuleSet(
    val value: RuleSet,
)

data class RuleSetValidationError(
    val violations: List<RuleSetViolation>,
)

sealed interface RuleSetViolation {
    data class FlagKeyMismatch(
        val mapKey: FlagKey,
        val flagKey: FlagKey,
    ) : RuleSetViolation

    data class DuplicateRuleId(
        val ruleId: RuleId,
        val flagKeys: List<FlagKey>,
    ) : RuleSetViolation

    data class ServeTypeMismatch(
        val flagKey: FlagKey,
        val ruleId: RuleId,
        val expected: RuleValueType,
        val actual: RuleValueType,
    ) : RuleSetViolation

    data class EmptyConditionBranch(
        val flagKey: FlagKey,
        val ruleId: RuleId,
        val branchKind: BranchKind,
    ) : RuleSetViolation

    data class InvalidPredicateValue(
        val flagKey: FlagKey,
        val ruleId: RuleId,
        val attributeKey: AttributeKey,
        val operator: Operator,
        val expectedTypes: Set<RuleValueType>,
        val actual: RuleValueType,
    ) : RuleSetViolation
}

enum class BranchKind {
    All,
    Any,
}

enum class RuleValueType {
    String,
    Number,
    Boolean,
    List,
    Null,
}

internal enum class RuleValueExpectation(
    val types: Set<RuleValueType>,
) {
    AnyComparable(setOf(RuleValueType.String, RuleValueType.Number, RuleValueType.Boolean, RuleValueType.Null)),
    ContainsElement(setOf(RuleValueType.String, RuleValueType.Number, RuleValueType.Boolean, RuleValueType.Null)),
    List(setOf(RuleValueType.List)),
    Number(setOf(RuleValueType.Number)),
    String(setOf(RuleValueType.String)),
    NoValue(setOf(RuleValueType.Null)),
}

fun RuleValue.type(): RuleValueType =
    when (this) {
        is RuleValue.StringValue -> RuleValueType.String
        is RuleValue.NumberValue -> RuleValueType.Number
        is RuleValue.BooleanValue -> RuleValueType.Boolean
        is RuleValue.ListValue -> RuleValueType.List
        RuleValue.NullValue -> RuleValueType.Null
    }

internal fun RuleValueExpectation.accepts(value: RuleValue): Boolean =
    when (this) {
        RuleValueExpectation.AnyComparable -> value !is RuleValue.ListValue
        RuleValueExpectation.ContainsElement -> value !is RuleValue.ListValue
        RuleValueExpectation.List -> value is RuleValue.ListValue
        RuleValueExpectation.Number -> value is RuleValue.NumberValue
        RuleValueExpectation.String -> value is RuleValue.StringValue
        RuleValueExpectation.NoValue -> value == RuleValue.NullValue
    }
