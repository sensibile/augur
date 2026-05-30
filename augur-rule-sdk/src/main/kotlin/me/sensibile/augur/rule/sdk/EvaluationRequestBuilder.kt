package me.sensibile.augur.rule.sdk

import me.sensibile.augur.rule.AttributeKey
import me.sensibile.augur.rule.EvaluationContext
import me.sensibile.augur.rule.EvaluationRequest
import me.sensibile.augur.rule.FlagKey
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.RuleValue
import me.sensibile.augur.rule.TargetKey
import me.sensibile.augur.rule.ValueObjectError
import me.sensibile.augur.rule.flatMap
import me.sensibile.augur.rule.map

sealed interface EvaluationRequestBuildError {
    data class InvalidValueObject(
        val error: ValueObjectError,
    ) : EvaluationRequestBuildError
}

fun evaluationRequest(
    flagKey: String,
    targetKey: String,
    configure: EvaluationAttributesBuilder.() -> Unit = {},
): Outcome<EvaluationRequestBuildError, EvaluationRequest> =
    attributes(configure).flatMap { attributes ->
        evaluationRequest(
            flagKey = flagKey,
            targetKey = targetKey,
            attributes = attributes,
        )
    }

fun evaluationRequest(
    flagKey: String,
    targetKey: String,
    attributes: EvaluationAttributes,
): Outcome<EvaluationRequestBuildError, EvaluationRequest> =
    FlagKey
        .of(flagKey)
        .mapError()
        .flatMap { flagKey ->
            TargetKey
                .of(targetKey)
                .mapError()
                .map { targetKey ->
                    EvaluationRequest(
                        flagKey = flagKey,
                        context =
                            EvaluationContext(
                                targetKey = targetKey,
                                attributes = attributes.values,
                            ),
                    )
                }
        }

fun attributes(configure: EvaluationAttributesBuilder.() -> Unit): Outcome<EvaluationRequestBuildError, EvaluationAttributes> {
    val builder = EvaluationAttributesBuilder()
    builder.configure()
    return builder.build()
}

@JvmInline
value class EvaluationAttributes internal constructor(
    val values: Map<AttributeKey, RuleValue>,
) {
    companion object {
        fun empty(): EvaluationAttributes = EvaluationAttributes(emptyMap())
    }
}

class EvaluationAttributesBuilder internal constructor() {
    private val values = linkedMapOf<AttributeKey, RuleValue>()
    private var error: EvaluationRequestBuildError? = null

    fun string(
        key: String,
        value: String,
    ) {
        put(key, RuleValue.string(value))
    }

    fun number(
        key: String,
        value: Double,
    ) {
        when (val result = RuleValue.number(value).mapError()) {
            is Outcome.Err -> recordError(result.error)
            is Outcome.Ok -> put(key, result.value)
        }
    }

    fun boolean(
        key: String,
        value: Boolean,
    ) {
        put(key, RuleValue.boolean(value))
    }

    fun list(
        key: String,
        values: List<RuleValue>,
    ) {
        put(key, RuleValue.list(values))
    }

    fun nullValue(key: String) {
        put(key, RuleValue.NullValue)
    }

    internal fun build(): Outcome<EvaluationRequestBuildError, EvaluationAttributes> =
        when (val error = error) {
            null -> Outcome.Ok(EvaluationAttributes(values.toMap()))
            else -> Outcome.Err(error)
        }

    private fun put(
        key: String,
        value: RuleValue,
    ) {
        if (error != null) {
            return
        }

        when (val result = AttributeKey.of(key).mapError()) {
            is Outcome.Err -> recordError(result.error)
            is Outcome.Ok -> values[result.value] = value
        }
    }

    private fun recordError(error: EvaluationRequestBuildError) {
        if (this.error == null) {
            this.error = error
        }
    }
}

private fun <A> Outcome<ValueObjectError, A>.mapError(): Outcome<EvaluationRequestBuildError, A> =
    when (this) {
        is Outcome.Err -> Outcome.Err(EvaluationRequestBuildError.InvalidValueObject(error))
        is Outcome.Ok -> Outcome.Ok(value)
    }
