package me.sensibile.augur.rule.sdk

import me.sensibile.augur.rule.EvaluationError
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.RuleEngine
import me.sensibile.augur.rule.RuleValue
import me.sensibile.augur.rule.TypedEvaluationDecision
import me.sensibile.augur.rule.ValidRuleSet
import me.sensibile.augur.rule.flatMap

class AugurEvaluator private constructor(
    private val ruleSet: ValidRuleSet,
) {
    fun evaluateBoolean(
        flagKey: String,
        targetKey: String,
        configure: EvaluationAttributesBuilder.() -> Unit = {},
    ): Outcome<AugurEvaluationError, TypedEvaluationDecision<Boolean>> =
        request(flagKey, targetKey, configure)
            .flatMap { request ->
                RuleEngine.evaluateBoolean(ruleSet, request).mapEvaluationError()
            }

    fun evaluateString(
        flagKey: String,
        targetKey: String,
        configure: EvaluationAttributesBuilder.() -> Unit = {},
    ): Outcome<AugurEvaluationError, TypedEvaluationDecision<String>> =
        request(flagKey, targetKey, configure)
            .flatMap { request ->
                RuleEngine.evaluateString(ruleSet, request).mapEvaluationError()
            }

    fun evaluateNumber(
        flagKey: String,
        targetKey: String,
        configure: EvaluationAttributesBuilder.() -> Unit = {},
    ): Outcome<AugurEvaluationError, TypedEvaluationDecision<Double>> =
        request(flagKey, targetKey, configure)
            .flatMap { request ->
                RuleEngine.evaluateNumber(ruleSet, request).mapEvaluationError()
            }

    fun evaluateList(
        flagKey: String,
        targetKey: String,
        configure: EvaluationAttributesBuilder.() -> Unit = {},
    ): Outcome<AugurEvaluationError, TypedEvaluationDecision<List<RuleValue>>> =
        request(flagKey, targetKey, configure)
            .flatMap { request ->
                RuleEngine.evaluateList(ruleSet, request).mapEvaluationError()
            }

    private fun request(
        flagKey: String,
        targetKey: String,
        configure: EvaluationAttributesBuilder.() -> Unit,
    ): Outcome<AugurEvaluationError, me.sensibile.augur.rule.EvaluationRequest> =
        evaluationRequest(
            flagKey = flagKey,
            targetKey = targetKey,
            configure = configure,
        ).mapRequestError()

    companion object {
        fun of(ruleSet: ValidRuleSet): AugurEvaluator = AugurEvaluator(ruleSet)
    }
}

sealed interface AugurEvaluationError {
    data class InvalidRequest(
        val error: EvaluationRequestBuildError,
    ) : AugurEvaluationError

    data class EvaluationFailed(
        val error: EvaluationError,
    ) : AugurEvaluationError
}

private fun <A> Outcome<EvaluationRequestBuildError, A>.mapRequestError(): Outcome<AugurEvaluationError, A> =
    when (this) {
        is Outcome.Err -> Outcome.Err(AugurEvaluationError.InvalidRequest(error))
        is Outcome.Ok -> Outcome.Ok(value)
    }

private fun <A> Outcome<EvaluationError, A>.mapEvaluationError(): Outcome<AugurEvaluationError, A> =
    when (this) {
        is Outcome.Err -> Outcome.Err(AugurEvaluationError.EvaluationFailed(error))
        is Outcome.Ok -> Outcome.Ok(value)
    }
