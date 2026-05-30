package me.sensibile.augur.rule.json

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import me.sensibile.augur.rule.AttributeKey
import me.sensibile.augur.rule.Condition
import me.sensibile.augur.rule.Flag
import me.sensibile.augur.rule.FlagKey
import me.sensibile.augur.rule.Operator
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.Rule
import me.sensibile.augur.rule.RuleId
import me.sensibile.augur.rule.RuleSet
import me.sensibile.augur.rule.RuleSetValidationError
import me.sensibile.augur.rule.RuleSetValidator
import me.sensibile.augur.rule.RuleSetVersion
import me.sensibile.augur.rule.RuleValue
import me.sensibile.augur.rule.RuleValueType
import me.sensibile.augur.rule.ValidRuleSet
import me.sensibile.augur.rule.ValueObjectError
import me.sensibile.augur.rule.flatMap
import me.sensibile.augur.rule.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private typealias ConditionOutcome = Outcome<RuleJsonError, Condition>
private typealias RuleListOutcome = Outcome<RuleJsonError, List<Rule>>
private typealias TraverseOutcome<B> = Outcome<RuleJsonError, List<B>>

object RuleJson {
    private val json =
        Json {
            classDiscriminator = "type"
            encodeDefaults = true
            explicitNulls = true
            prettyPrint = true
        }

    fun decodeRuleSet(value: String): Outcome<RuleJsonError, RuleSet> =
        try {
            RuleSetJson.serializer()
            json
                .decodeFromString<RuleSetJson>(value)
                .toDomain()
        } catch (exception: IllegalArgumentException) {
            Outcome.Err(RuleJsonError.InvalidJson(exception.message.orEmpty()))
        }

    fun decodeValidRuleSet(value: String): Outcome<RuleJsonError, ValidRuleSet> =
        decodeRuleSet(value).flatMap { ruleSet ->
            RuleSetValidator.validate(ruleSet).mapValidationError()
        }

    fun encodeRuleSet(ruleSet: RuleSet): String =
        json.encodeToString(
            RuleSetJson.serializer(),
            RuleSetJson.fromDomain(ruleSet),
        )
}

sealed interface RuleJsonError {
    data class InvalidJson(
        val message: String,
    ) : RuleJsonError

    data class InvalidValueObject(
        val error: ValueObjectError,
    ) : RuleJsonError

    data class InvalidRuleValue(
        val type: RuleValueType,
    ) : RuleJsonError

    data class InvalidRuleSet(
        val error: RuleSetValidationError,
    ) : RuleJsonError

    data class DuplicateFlagKey(
        val key: FlagKey,
    ) : RuleJsonError
}

@Serializable
private data class RuleSetJson(
    val version: Long,
    val flags: List<FlagJson>,
) {
    fun toDomain(): Outcome<RuleJsonError, RuleSet> {
        val version = RuleSetVersion.of(version).mapError()
        val flags = flags.traverse { flag -> flag.toDomain() }

        return version
            .zip(flags) { version, flags -> version to flags }
            .flatMap { (version, flags) ->
                flags.toRuleSet(version)
            }
    }

    private fun List<Flag>.toRuleSet(version: RuleSetVersion): Outcome<RuleJsonError, RuleSet> {
        val duplicatedKey =
            groupingBy { flag -> flag.key }
                .eachCount()
                .entries
                .firstOrNull { (_, count) -> count > 1 }
                ?.key

        return if (duplicatedKey == null) {
            Outcome.Ok(
                RuleSet(
                    version = version,
                    flags = associateBy { flag -> flag.key },
                ),
            )
        } else {
            Outcome.Err(RuleJsonError.DuplicateFlagKey(duplicatedKey))
        }
    }

    companion object {
        fun fromDomain(ruleSet: RuleSet): RuleSetJson =
            RuleSetJson(
                version = ruleSet.version.value,
                flags = ruleSet.flags.values.map(FlagJson::fromDomain),
            )
    }
}

@Serializable
private data class FlagJson(
    val key: String,
    val enabled: Boolean,
    val defaultValue: JsonElement,
    val rules: List<RuleJsonDto>,
) {
    fun toDomain(): Outcome<RuleJsonError, Flag> =
        FlagKey
            .of(key)
            .mapError()
            .flatMap { key ->
                defaultValue
                    .toRuleValue()
                    .flatMap { defaultValue ->
                        rules.toDomainRules().map { rules ->
                            Flag(
                                key = key,
                                enabled = enabled,
                                defaultValue = defaultValue,
                                rules = rules,
                            )
                        }
                    }
            }

    companion object {
        fun fromDomain(flag: Flag): FlagJson =
            FlagJson(
                key = flag.key.value,
                enabled = flag.enabled,
                defaultValue = flag.defaultValue.toJsonElement(),
                rules = flag.rules.map(RuleJsonDto::fromDomain),
            )
    }
}

@Serializable
private data class RuleJsonDto(
    val id: String,
    val condition: ConditionJson,
    val serve: JsonElement,
) {
    @OptIn(ExperimentalUuidApi::class)
    fun toDomain(): Outcome<RuleJsonError, Rule> =
        RuleId
            .of(Uuid.parse(id))
            .mapError()
            .flatMap { id ->
                condition.toDomain().flatMap { condition ->
                    serve.toRuleValue().map { serve ->
                        Rule(
                            id = id,
                            condition = condition,
                            serve = serve,
                        )
                    }
                }
            }

    companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun fromDomain(rule: Rule): RuleJsonDto =
            RuleJsonDto(
                id = rule.id.value.toString(),
                condition = ConditionJson.fromDomain(rule.condition),
                serve = rule.serve.toJsonElement(),
            )
    }
}

@Serializable
private sealed interface ConditionJson {
    fun toDomain(): ConditionOutcome

    @Serializable
    @SerialName("all")
    data class All(
        val conditions: List<ConditionJson>,
    ) : ConditionJson {
        override fun toDomain(): ConditionOutcome = conditions.toDomainConditions().map(Condition::All)
    }

    @Serializable
    @SerialName("any")
    data class Any(
        val conditions: List<ConditionJson>,
    ) : ConditionJson {
        override fun toDomain(): ConditionOutcome = conditions.toDomainConditions().map(Condition::Any)
    }

    @Serializable
    @SerialName("not")
    data class Not(
        val condition: ConditionJson,
    ) : ConditionJson {
        override fun toDomain(): ConditionOutcome = condition.toDomain().map(Condition::Not)
    }

    @Serializable
    @SerialName("predicate")
    data class Predicate(
        val field: String,
        val op: Operator,
        val value: JsonElement = JsonNull,
    ) : ConditionJson {
        override fun toDomain(): ConditionOutcome =
            AttributeKey
                .of(field)
                .mapError()
                .flatMap { field ->
                    value.toRuleValue().map { value ->
                        Condition.Predicate(
                            attributeKey = field,
                            operator = op,
                            value = value,
                        )
                    }
                }
    }

    companion object {
        fun fromDomain(condition: Condition): ConditionJson =
            when (condition) {
                is Condition.All -> {
                    All(condition.conditions.map(::fromDomain))
                }

                is Condition.Any -> {
                    Any(condition.conditions.map(::fromDomain))
                }

                is Condition.Not -> {
                    Not(fromDomain(condition.condition))
                }

                is Condition.Predicate -> {
                    Predicate(
                        field = condition.attributeKey.value,
                        op = condition.operator,
                        value = condition.value.toJsonElement(),
                    )
                }
            }
    }
}

private fun List<RuleJsonDto>.toDomainRules(): RuleListOutcome = traverse { rule -> rule.toDomain() }

private fun List<ConditionJson>.toDomainConditions(): Outcome<RuleJsonError, List<Condition>> =
    traverse { condition -> condition.toDomain() }

private inline fun <A, B> List<A>.traverse(transform: (A) -> Outcome<RuleJsonError, B>): TraverseOutcome<B> {
    val values = mutableListOf<B>()
    for (value in this) {
        when (val result = transform(value)) {
            is Outcome.Err -> return result
            is Outcome.Ok -> values += result.value
        }
    }
    return Outcome.Ok(values)
}

private fun JsonElement.toRuleValue(): Outcome<RuleJsonError, RuleValue> =
    when {
        this is JsonNull -> {
            Outcome.Ok(RuleValue.NullValue)
        }

        this is JsonPrimitive && isString -> {
            Outcome.Ok(RuleValue.string(content))
        }

        this is JsonPrimitive && booleanOrNull != null -> {
            Outcome.Ok(RuleValue.boolean(booleanOrNull ?: false))
        }

        this is JsonPrimitive && doubleOrNull != null -> {
            RuleValue.number(doubleOrNull ?: 0.0).mapError()
        }

        this is kotlinx.serialization.json.JsonArray -> {
            jsonArray.map { value -> value.toRuleValue() }.traverseValue()
        }

        else -> {
            Outcome.Err(RuleJsonError.InvalidRuleValue(type = RuleValueType.Null))
        }
    }

private fun List<Outcome<RuleJsonError, RuleValue>>.traverseValue(): Outcome<RuleJsonError, RuleValue.ListValue> {
    val values = mutableListOf<RuleValue>()
    for (value in this) {
        when (value) {
            is Outcome.Err -> return value
            is Outcome.Ok -> values += value.value
        }
    }
    return Outcome.Ok(RuleValue.list(values))
}

private fun RuleValue.toJsonElement(): JsonElement =
    when (this) {
        is RuleValue.BooleanValue -> {
            JsonPrimitive(value)
        }

        is RuleValue.ListValue -> {
            kotlinx.serialization.json.JsonArray(
                values.map(RuleValue::toJsonElement),
            )
        }

        RuleValue.NullValue -> {
            JsonNull
        }

        is RuleValue.NumberValue -> {
            JsonPrimitive(value)
        }

        is RuleValue.StringValue -> {
            JsonPrimitive(value)
        }
    }

private fun <A> Outcome<ValueObjectError, A>.mapError(): Outcome<RuleJsonError, A> =
    when (this) {
        is Outcome.Err -> Outcome.Err(RuleJsonError.InvalidValueObject(error))
        is Outcome.Ok -> Outcome.Ok(value)
    }

private fun Outcome<RuleSetValidationError, ValidRuleSet>.mapValidationError(): Outcome<RuleJsonError, ValidRuleSet> =
    when (this) {
        is Outcome.Err -> Outcome.Err(RuleJsonError.InvalidRuleSet(error))
        is Outcome.Ok -> Outcome.Ok(value)
    }

private inline fun <A, B, C> Outcome<RuleJsonError, A>.zip(
    other: Outcome<RuleJsonError, B>,
    transform: (A, B) -> C,
): Outcome<RuleJsonError, C> =
    when (this) {
        is Outcome.Err -> {
            this
        }

        is Outcome.Ok -> {
            when (other) {
                is Outcome.Err -> other
                is Outcome.Ok -> Outcome.Ok(transform(value, other.value))
            }
        }
    }
