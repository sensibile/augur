@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package me.sensibile.augur.rule.sdk

import me.sensibile.augur.rule.AttributeKey
import me.sensibile.augur.rule.Condition
import me.sensibile.augur.rule.Flag
import me.sensibile.augur.rule.FlagKey
import me.sensibile.augur.rule.Operator
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.Rule
import me.sensibile.augur.rule.RuleId
import me.sensibile.augur.rule.RuleSet
import me.sensibile.augur.rule.RuleSetValidator
import me.sensibile.augur.rule.RuleSetVersion
import me.sensibile.augur.rule.RuleValue
import me.sensibile.augur.rule.TargetKey
import kotlin.uuid.Uuid

internal fun validCheckoutRuleSet() =
    valid(
        RuleSet(
            version = version(1),
            flags =
                mapOf(
                    flagKey("new_checkout") to
                        Flag(
                            key = flagKey("new_checkout"),
                            enabled = true,
                            defaultValue = RuleValue.boolean(false),
                            rules =
                                listOf(
                                    Rule(
                                        id = ruleId("01890f2e-7cc3-7cc3-8c4f-123456789abc"),
                                        condition =
                                            Condition.All(
                                                listOf(
                                                    Condition.Predicate(
                                                        attributeKey = attributeKey("country"),
                                                        operator = Operator.Eq,
                                                        value = RuleValue.string("KR"),
                                                    ),
                                                    Condition.Predicate(
                                                        attributeKey = attributeKey("plan"),
                                                        operator = Operator.In,
                                                        value =
                                                            RuleValue.list(
                                                                listOf(
                                                                    RuleValue.string("pro"),
                                                                    RuleValue.string("team"),
                                                                ),
                                                            ),
                                                    ),
                                                ),
                                            ),
                                        serve = RuleValue.boolean(true),
                                    ),
                                ),
                        ),
                ),
        ),
    )

internal fun validStringRuleSet() =
    valid(
        RuleSet(
            version = version(1),
            flags =
                mapOf(
                    flagKey("copy_text") to
                        Flag(
                            key = flagKey("copy_text"),
                            enabled = true,
                            defaultValue = RuleValue.string("control"),
                            rules = emptyList(),
                        ),
                ),
        ),
    )

internal fun validNumberRuleSet() =
    valid(
        RuleSet(
            version = version(1),
            flags =
                mapOf(
                    flagKey("discount_percent") to
                        Flag(
                            key = flagKey("discount_percent"),
                            enabled = true,
                            defaultValue = number(0.0),
                            rules =
                                listOf(
                                    Rule(
                                        id = ruleId("01890f2e-7cc3-7cc3-8c4f-123456789abd"),
                                        condition =
                                            Condition.Predicate(
                                                attributeKey = attributeKey("plan"),
                                                operator = Operator.Eq,
                                                value = RuleValue.string("team"),
                                            ),
                                        serve = number(15.0),
                                    ),
                                ),
                        ),
                ),
        ),
    )

internal fun validListRuleSet() =
    valid(
        RuleSet(
            version = version(1),
            flags =
                mapOf(
                    flagKey("enabled_regions") to
                        Flag(
                            key = flagKey("enabled_regions"),
                            enabled = true,
                            defaultValue = RuleValue.list(listOf(RuleValue.string("US"))),
                            rules =
                                listOf(
                                    Rule(
                                        id = ruleId("01890f2e-7cc3-7cc3-8c4f-123456789abe"),
                                        condition =
                                            Condition.Predicate(
                                                attributeKey = attributeKey("tier"),
                                                operator = Operator.Eq,
                                                value = RuleValue.string("enterprise"),
                                            ),
                                        serve = RuleValue.list(listOf(RuleValue.string("KR"), RuleValue.string("JP"))),
                                    ),
                                ),
                        ),
                ),
        ),
    )

internal fun flagKey(value: String): FlagKey =
    when (val result = FlagKey.of(value)) {
        is Outcome.Err -> error("Invalid test flag key: $value")
        is Outcome.Ok -> result.value
    }

internal fun targetKey(value: String): TargetKey =
    when (val result = TargetKey.of(value)) {
        is Outcome.Err -> error("Invalid test target key: $value")
        is Outcome.Ok -> result.value
    }

internal fun attributeKey(value: String): AttributeKey =
    when (val result = AttributeKey.of(value)) {
        is Outcome.Err -> error("Invalid test attribute key: $value")
        is Outcome.Ok -> result.value
    }

internal fun number(value: Double): RuleValue.NumberValue =
    when (val result = RuleValue.number(value)) {
        is Outcome.Err -> error("Invalid test number value: $value")
        is Outcome.Ok -> result.value
    }

private fun version(value: Long): RuleSetVersion =
    when (val result = RuleSetVersion.of(value)) {
        is Outcome.Err -> error("Invalid test version: $value")
        is Outcome.Ok -> result.value
    }

private fun ruleId(value: String): RuleId =
    when (val result = RuleId.of(Uuid.parse(value))) {
        is Outcome.Err -> error("Invalid test rule id: $value")
        is Outcome.Ok -> result.value
    }

private fun valid(ruleSet: RuleSet) =
    when (val result = RuleSetValidator.validate(ruleSet)) {
        is Outcome.Err -> error("Invalid test ruleset: ${result.error}")
        is Outcome.Ok -> result.value
    }
