package me.sensibile.augur.rule.sdk

import me.sensibile.augur.rule.EvaluationError
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.RuleEngine
import me.sensibile.augur.rule.RuleSetSnapshot
import me.sensibile.augur.rule.RuleValue
import me.sensibile.augur.rule.TypedEvaluationDecision
import me.sensibile.augur.rule.flatMap

class AugurEvaluator private constructor(
    private val snapshot: RuleSetSnapshot,
) {
    fun evaluateBoolean(
        flagKey: String,
        targetKey: String,
        configure: EvaluationAttributesBuilder.() -> Unit = {},
    ): Outcome<AugurEvaluationError, TypedEvaluationDecision<Boolean>> =
        request(flagKey, targetKey, configure)
            .flatMap { request ->
                RuleEngine.evaluateBoolean(snapshot, request).mapEvaluationError()
            }

    fun evaluateString(
        flagKey: String,
        targetKey: String,
        configure: EvaluationAttributesBuilder.() -> Unit = {},
    ): Outcome<AugurEvaluationError, TypedEvaluationDecision<String>> =
        request(flagKey, targetKey, configure)
            .flatMap { request ->
                RuleEngine.evaluateString(snapshot, request).mapEvaluationError()
            }

    fun evaluateNumber(
        flagKey: String,
        targetKey: String,
        configure: EvaluationAttributesBuilder.() -> Unit = {},
    ): Outcome<AugurEvaluationError, TypedEvaluationDecision<Double>> =
        request(flagKey, targetKey, configure)
            .flatMap { request ->
                RuleEngine.evaluateNumber(snapshot, request).mapEvaluationError()
            }

    fun evaluateList(
        flagKey: String,
        targetKey: String,
        configure: EvaluationAttributesBuilder.() -> Unit = {},
    ): Outcome<AugurEvaluationError, TypedEvaluationDecision<List<RuleValue>>> =
        request(flagKey, targetKey, configure)
            .flatMap { request ->
                RuleEngine.evaluateList(snapshot, request).mapEvaluationError()
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
        fun of(snapshot: RuleSetSnapshot): AugurEvaluator = AugurEvaluator(snapshot)
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
