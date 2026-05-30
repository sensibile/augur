package me.sensibile.augur.rule

sealed interface Condition {
    data class All(
        val conditions: List<Condition>,
    ) : Condition

    data class Any(
        val conditions: List<Condition>,
    ) : Condition

    data class Not(
        val condition: Condition,
    ) : Condition

    data class Predicate(
        val attributeKey: AttributeKey,
        val operator: Operator,
        val value: RuleValue = RuleValue.NullValue,
    ) : Condition
}

enum class Operator {
    Eq,
    NotEq,
    In,
    NotIn,
    GreaterThan,
    GreaterThanOrEqual,
    LessThan,
    LessThanOrEqual,
    Exists,
    Missing,
    Contains,
    StartsWith,
    EndsWith,
}
