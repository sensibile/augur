@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package me.sensibile.augur.rule

import kotlin.uuid.Uuid

internal object RuleFixtures {
    private const val DEFAULT_RULE_ID = "01890f2e-7cc3-7cc3-8c4f-123456789abc"

    fun ruleSet(vararg flags: Flag): RuleSet =
        RuleSet(
            version = version(1),
            flags = flags.associateBy { it.key },
        )

    fun valid(ruleSet: RuleSet): RuleSetSnapshot =
        when (val validation = RuleSetValidator.validate(ruleSet)) {
            is Outcome.Err -> error("Invalid test ruleset: ${validation.error}")
            is Outcome.Ok -> validation.value
        }

    fun flag(
        key: FlagKey,
        enabled: Boolean = true,
        defaultValue: RuleValue = bool(false),
        rules: List<Rule> = listOf(rule()),
    ): Flag =
        Flag(
            key = key,
            enabled = enabled,
            defaultValue = defaultValue,
            rules = rules,
        )

    fun rule(
        id: RuleId = ruleId(DEFAULT_RULE_ID),
        condition: Condition = predicate("country", Operator.Eq, string("KR")),
        serve: RuleValue = bool(true),
    ): Rule =
        Rule(
            id = id,
            condition = condition,
            serve = serve,
        )

    fun request(
        flagKey: String,
        attributes: Map<AttributeKey, RuleValue> = emptyMap(),
    ): EvaluationRequest =
        EvaluationRequest(
            flagKey = flagKey(flagKey),
            context =
                EvaluationContext(
                    targetKey = targetKey("user-1"),
                    attributes = attributes,
                ),
        )

    fun predicate(
        attributeKey: String,
        operator: Operator,
        value: RuleValue = RuleValue.NullValue,
    ): Condition.Predicate =
        Condition.Predicate(
            attributeKey = attributeKey(attributeKey),
            operator = operator,
            value = value,
        )

    fun bool(value: Boolean): RuleValue.BooleanValue = RuleValue.boolean(value)

    fun string(value: String): RuleValue.StringValue = RuleValue.string(value)

    fun number(value: Double): RuleValue.NumberValue =
        when (val result = RuleValue.number(value)) {
            is Outcome.Err -> error("Invalid test number value: $value")
            is Outcome.Ok -> result.value
        }

    fun list(vararg values: RuleValue): RuleValue.ListValue = RuleValue.list(values.toList())

    fun flagKey(value: String): FlagKey =
        when (val result = FlagKey.of(value)) {
            is Outcome.Err -> error("Invalid test flag key: $value")
            is Outcome.Ok -> result.value
        }

    fun attributeKey(value: String): AttributeKey =
        when (val result = AttributeKey.of(value)) {
            is Outcome.Err -> error("Invalid test attribute key: $value")
            is Outcome.Ok -> result.value
        }

    fun targetKey(value: String): TargetKey =
        when (val result = TargetKey.of(value)) {
            is Outcome.Err -> error("Invalid test target key: $value")
            is Outcome.Ok -> result.value
        }

    fun version(value: Long): RuleSetVersion =
        when (val result = RuleSetVersion.of(value)) {
            is Outcome.Err -> error("Invalid test version: $value")
            is Outcome.Ok -> result.value
        }

    fun ruleId(value: String): RuleId =
        when (val result = RuleId.of(Uuid.parse(value))) {
            is Outcome.Err -> error("Invalid test rule id: $value")
            is Outcome.Ok -> result.value
        }
}
