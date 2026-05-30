package me.sensibile.augur.rule

data class EvaluationRequest(
    val flagKey: FlagKey,
    val context: EvaluationContext,
)

data class EvaluationContext(
    val targetKey: TargetKey,
    val attributes: Map<AttributeKey, RuleValue> = emptyMap(),
)

data class EvaluationDecision(
    val flagKey: FlagKey,
    val value: RuleValue,
    val reason: EvaluationReason,
    val matchedRuleId: RuleId? = null,
    val ruleSetVersion: RuleSetVersion? = null,
    val trace: EvaluationTrace = EvaluationTrace.empty(),
)

enum class EvaluationReason {
    RuleMatch,
    Default,
    FlagDisabled,
    FlagNotFound,
}

sealed interface EvaluationError {
    data class InvalidRuleSet(
        val validationError: RuleSetValidationError,
    ) : EvaluationError
}

data class EvaluationTrace(
    val rules: List<RuleEvaluationTrace>,
) {
    companion object {
        fun empty(): EvaluationTrace = EvaluationTrace(emptyList())
    }
}

data class RuleEvaluationTrace(
    val ruleId: RuleId,
    val matched: Boolean,
)

object RuleEngine {
    fun evaluate(
        ruleSet: RuleSet,
        request: EvaluationRequest,
    ): Outcome<EvaluationError, EvaluationDecision> =
        when (val validation = RuleSetValidator.validate(ruleSet)) {
            is Outcome.Err -> Outcome.Err(EvaluationError.InvalidRuleSet(validation.error))
            is Outcome.Ok -> evaluate(validation.value, request)
        }

    fun evaluate(
        ruleSet: ValidRuleSet,
        request: EvaluationRequest,
    ): Outcome<EvaluationError, EvaluationDecision> = RuleEvaluator.evaluate(ruleSet, request)
}

private object RuleEvaluator {
    fun evaluate(
        ruleSet: ValidRuleSet,
        request: EvaluationRequest,
    ): Outcome<EvaluationError, EvaluationDecision> {
        val validatedRuleSet = ruleSet.value
        val flag =
            validatedRuleSet.flags[request.flagKey]

        val decision =
            when {
                flag == null -> flagNotFound(request, validatedRuleSet)
                !flag.enabled -> flagDisabled(request, validatedRuleSet, flag)
                else -> evaluateEnabledFlag(request, validatedRuleSet, flag)
            }

        return Outcome.Ok(decision)
    }

    private fun flagNotFound(
        request: EvaluationRequest,
        ruleSet: RuleSet,
    ): EvaluationDecision =
        EvaluationDecision(
            flagKey = request.flagKey,
            value = RuleValue.NullValue,
            reason = EvaluationReason.FlagNotFound,
            ruleSetVersion = ruleSet.version,
        )

    private fun flagDisabled(
        request: EvaluationRequest,
        ruleSet: RuleSet,
        flag: Flag,
    ): EvaluationDecision =
        EvaluationDecision(
            flagKey = request.flagKey,
            value = flag.defaultValue,
            reason = EvaluationReason.FlagDisabled,
            ruleSetVersion = ruleSet.version,
        )

    private fun evaluateEnabledFlag(
        request: EvaluationRequest,
        ruleSet: RuleSet,
        flag: Flag,
    ): EvaluationDecision {
        val ruleTraces = mutableListOf<RuleEvaluationTrace>()
        val matchedRule =
            flag.rules.firstOrNull { rule ->
                val matched = ConditionMatcher.matches(rule.condition, request.context)
                ruleTraces += RuleEvaluationTrace(rule.id, matched)
                matched
            }

        return if (matchedRule == null) {
            EvaluationDecision(
                flagKey = request.flagKey,
                value = flag.defaultValue,
                reason = EvaluationReason.Default,
                ruleSetVersion = ruleSet.version,
                trace = EvaluationTrace(ruleTraces),
            )
        } else {
            EvaluationDecision(
                flagKey = request.flagKey,
                value = matchedRule.serve,
                reason = EvaluationReason.RuleMatch,
                matchedRuleId = matchedRule.id,
                ruleSetVersion = ruleSet.version,
                trace = EvaluationTrace(ruleTraces),
            )
        }
    }
}

private object ConditionMatcher {
    fun matches(
        condition: Condition,
        context: EvaluationContext,
    ): Boolean =
        when (condition) {
            is Condition.All -> condition.conditions.all { matches(it, context) }
            is Condition.Any -> condition.conditions.any { matches(it, context) }
            is Condition.Not -> !matches(condition.condition, context)
            is Condition.Predicate -> matches(condition, context)
        }

    private fun matches(
        predicate: Condition.Predicate,
        context: EvaluationContext,
    ): Boolean {
        val actual = context.attributes[predicate.attributeKey]
        return predicate.operator.matcher.matches(actual, predicate.value)
    }

    private val Operator.matcher: PredicateMatcher
        get() =
            when (this) {
                Operator.Eq -> PredicateMatcher.Binary { actual, expected -> actual == expected }
                Operator.NotEq -> PredicateMatcher.Binary { actual, expected -> actual != null && actual != expected }
                Operator.In -> PredicateMatcher.Binary(::inList)
                Operator.NotIn -> PredicateMatcher.Binary(::notInList)
                Operator.GreaterThan -> PredicateMatcher.Number { actual, expected -> actual > expected }
                Operator.GreaterThanOrEqual -> PredicateMatcher.Number { actual, expected -> actual >= expected }
                Operator.LessThan -> PredicateMatcher.Number { actual, expected -> actual < expected }
                Operator.LessThanOrEqual -> PredicateMatcher.Number { actual, expected -> actual <= expected }
                Operator.Exists -> PredicateMatcher.Unary { actual -> actual != null }
                Operator.Missing -> PredicateMatcher.Unary { actual -> actual == null }
                Operator.Contains -> PredicateMatcher.Binary(::contains)
                Operator.StartsWith -> PredicateMatcher.String { actual, expected -> actual.startsWith(expected) }
                Operator.EndsWith -> PredicateMatcher.String { actual, expected -> actual.endsWith(expected) }
            }
}

private sealed interface PredicateMatcher {
    fun matches(
        actual: RuleValue?,
        expected: RuleValue,
    ): Boolean

    data class Unary(
        val predicate: (RuleValue?) -> Boolean,
    ) : PredicateMatcher {
        override fun matches(
            actual: RuleValue?,
            expected: RuleValue,
        ): Boolean = predicate(actual)
    }

    data class Binary(
        val predicate: (RuleValue?, RuleValue) -> Boolean,
    ) : PredicateMatcher {
        override fun matches(
            actual: RuleValue?,
            expected: RuleValue,
        ): Boolean = predicate(actual, expected)
    }

    data class Number(
        val predicate: (Double, Double) -> Boolean,
    ) : PredicateMatcher {
        override fun matches(
            actual: RuleValue?,
            expected: RuleValue,
        ): Boolean =
            actual is RuleValue.NumberValue &&
                expected is RuleValue.NumberValue &&
                predicate(actual.value, expected.value)
    }

    data class String(
        val predicate: (kotlin.String, kotlin.String) -> Boolean,
    ) : PredicateMatcher {
        override fun matches(
            actual: RuleValue?,
            expected: RuleValue,
        ): Boolean =
            actual is RuleValue.StringValue &&
                expected is RuleValue.StringValue &&
                predicate(actual.value, expected.value)
    }
}

private fun inList(
    actual: RuleValue?,
    expected: RuleValue,
): Boolean =
    expected is RuleValue.ListValue &&
        expected.values.contains(actual)

private fun notInList(
    actual: RuleValue?,
    expected: RuleValue,
): Boolean = actual != null && !inList(actual, expected)

private fun contains(
    actual: RuleValue?,
    expected: RuleValue,
): Boolean =
    when (actual) {
        is RuleValue.ListValue -> actual.values.contains(expected)
        is RuleValue.StringValue -> expected is RuleValue.StringValue && actual.value.contains(expected.value)
        else -> false
    }
