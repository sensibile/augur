@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package me.sensibile.augur.rule.json

import me.sensibile.augur.rule.AttributeKey
import me.sensibile.augur.rule.Condition
import me.sensibile.augur.rule.EvaluationContext
import me.sensibile.augur.rule.EvaluationRequest
import me.sensibile.augur.rule.Flag
import me.sensibile.augur.rule.FlagKey
import me.sensibile.augur.rule.Operator
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.Rule
import me.sensibile.augur.rule.RuleId
import me.sensibile.augur.rule.RuleSet
import me.sensibile.augur.rule.RuleSetVersion
import me.sensibile.augur.rule.RuleValue
import me.sensibile.augur.rule.TargetKey
import kotlin.uuid.Uuid

internal fun checkoutRuleSet(): RuleSet =
    RuleSet(
        version = version(1),
        flags =
            mapOf(
                flagKey("new_checkout") to
                    Flag(
                        key = flagKey("new_checkout"),
                        enabled = true,
                        defaultValue = bool(false),
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
                                                    value = string("KR"),
                                                ),
                                                Condition.Predicate(
                                                    attributeKey = attributeKey("plan"),
                                                    operator = Operator.In,
                                                    value = list(string("pro"), string("team")),
                                                ),
                                            ),
                                        ),
                                    serve = bool(true),
                                ),
                            ),
                    ),
            ),
    )

internal fun bool(value: Boolean): RuleValue.BooleanValue = RuleValue.boolean(value)

internal fun string(value: String): RuleValue.StringValue = RuleValue.string(value)

internal fun number(value: Double): RuleValue.NumberValue =
    when (val result = RuleValue.number(value)) {
        is Outcome.Err -> error("Invalid test number: ${result.error}")
        is Outcome.Ok -> result.value
    }

internal fun list(vararg values: RuleValue): RuleValue.ListValue = RuleValue.list(values.toList())

internal fun sampleRuleSetJson(condition: String = matchedConditionJson()): String = ruleSetJson(condition = condition)

internal fun ruleSetJson(
    condition: String,
    version: Long = 1,
    ruleId: String = "01890f2e-7cc3-7cc3-8c4f-123456789abc",
    defaultValue: String = "false",
    serve: String = "true",
): String =
    """
    {
      "version": $version,
      "flags": [
        {
          "key": "new_checkout",
          "enabled": true,
          "defaultValue": $defaultValue,
          "rules": [
            {
              "id": "$ruleId",
              "serve": $serve,
              "condition": $condition
            }
          ]
        }
      ]
    }
    """.trimIndent()

internal fun flagJson(): String =
    """
    {
      "key": "new_checkout",
      "enabled": true,
      "defaultValue": false,
      "rules": []
    }
    """.trimIndent()

internal fun ruleJson(): String =
    """
    {
      "id": "01890f2e-7cc3-7cc3-8c4f-123456789abc",
      "serve": true,
      "condition": ${predicateJson()}
    }
    """.trimIndent()

internal fun matchedConditionJson(): String =
    """
    {
      "type": "all",
      "conditions": [
        {
          "type": "predicate",
          "field": "country",
          "op": "Eq",
          "value": "KR"
        },
        {
          "type": "predicate",
          "field": "plan",
          "op": "In",
          "value": ["pro", "team"]
        }
      ]
    }
    """.trimIndent()

internal fun predicateJson(
    field: String = "country",
    op: String = "Eq",
    value: String = """"KR"""",
): String =
    """
    {
      "type": "predicate",
      "field": "$field",
      "op": "$op",
      "value": $value
    }
    """.trimIndent()

internal fun request(
    flagKey: String,
    attributes: Map<AttributeKey, RuleValue>,
): EvaluationRequest =
    EvaluationRequest(
        flagKey = flagKey(flagKey),
        context =
            EvaluationContext(
                targetKey = targetKey("user-1"),
                attributes = attributes,
            ),
    )

internal fun flagKey(value: String): FlagKey =
    when (val result = FlagKey.of(value)) {
        is Outcome.Err -> error("Invalid test flag key: $value")
        is Outcome.Ok -> result.value
    }

internal fun attributeKey(value: String): AttributeKey =
    when (val result = AttributeKey.of(value)) {
        is Outcome.Err -> error("Invalid test attribute key: $value")
        is Outcome.Ok -> result.value
    }

private fun targetKey(value: String): TargetKey =
    when (val result = TargetKey.of(value)) {
        is Outcome.Err -> error("Invalid test target key: $value")
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
