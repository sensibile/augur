package me.sensibile.augur.rule.json

import me.sensibile.augur.rule.AttributeKey
import me.sensibile.augur.rule.EvaluationContext
import me.sensibile.augur.rule.EvaluationReason
import me.sensibile.augur.rule.EvaluationRequest
import me.sensibile.augur.rule.FlagKey
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.RuleEngine
import me.sensibile.augur.rule.RuleValue
import me.sensibile.augur.rule.TargetKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RuleJsonEngineIntegrationTest {
    @Test
    fun `decodes valid json and evaluates matched rule`() {
        val ruleSet =
            when (val decoded = RuleJson.decodeValidRuleSet(sampleRuleSetJson())) {
                is Outcome.Err -> error("Expected valid ruleset: ${decoded.error}")
                is Outcome.Ok -> decoded.value
            }

        val actual =
            RuleEngine.evaluate(
                ruleSet = ruleSet,
                request =
                    request(
                        flagKey = "new_checkout",
                        attributes =
                            mapOf(
                                attributeKey("country") to RuleValue.string("KR"),
                                attributeKey("plan") to RuleValue.string("pro"),
                            ),
                    ),
            )

        assertEquals(EvaluationReason.RuleMatch, (actual as Outcome.Ok).value.reason)
        assertEquals(RuleValue.boolean(true), actual.value.value)
    }

    @Test
    fun `decodes valid json and evaluates default when no rule matches`() {
        val ruleSet =
            when (val decoded = RuleJson.decodeValidRuleSet(sampleRuleSetJson())) {
                is Outcome.Err -> error("Expected valid ruleset: ${decoded.error}")
                is Outcome.Ok -> decoded.value
            }

        val actual =
            RuleEngine.evaluate(
                ruleSet = ruleSet,
                request =
                    request(
                        flagKey = "new_checkout",
                        attributes =
                            mapOf(
                                attributeKey("country") to RuleValue.string("US"),
                                attributeKey("plan") to RuleValue.string("free"),
                            ),
                    ),
            )

        assertEquals(EvaluationReason.Default, (actual as Outcome.Ok).value.reason)
        assertEquals(RuleValue.boolean(false), actual.value.value)
    }

    @Test
    fun `does not evaluate invalid decoded ruleset`() {
        val json =
            sampleRuleSetJson(
                condition =
                    """
                    {
                      "type": "predicate",
                      "field": "age",
                      "op": "GreaterThan",
                      "value": "19"
                    }
                    """.trimIndent(),
            )

        val decoded = RuleJson.decodeValidRuleSet(json)

        assertIs<Outcome.Err<RuleJsonError.InvalidRuleSet>>(decoded)
    }

    private fun sampleRuleSetJson(condition: String = matchedConditionJson()): String =
        """
        {
          "version": 1,
          "flags": [
            {
              "key": "new_checkout",
              "enabled": true,
              "defaultValue": false,
              "rules": [
                {
                  "id": "01890f2e-7cc3-7cc3-8c4f-123456789abc",
                  "serve": true,
                  "condition": $condition
                }
              ]
            }
          ]
        }
        """.trimIndent()

    private fun matchedConditionJson(): String =
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

    private fun request(
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

    private fun flagKey(value: String): FlagKey =
        when (val result = FlagKey.of(value)) {
            is Outcome.Err -> error("Invalid test flag key: $value")
            is Outcome.Ok -> result.value
        }

    private fun attributeKey(value: String): AttributeKey =
        when (val result = AttributeKey.of(value)) {
            is Outcome.Err -> error("Invalid test attribute key: $value")
            is Outcome.Ok -> result.value
        }

    private fun targetKey(value: String): TargetKey =
        when (val result = TargetKey.of(value)) {
            is Outcome.Err -> error("Invalid test target key: $value")
            is Outcome.Ok -> result.value
        }
}
