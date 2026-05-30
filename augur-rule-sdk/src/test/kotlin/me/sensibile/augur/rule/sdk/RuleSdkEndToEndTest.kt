package me.sensibile.augur.rule.sdk

import me.sensibile.augur.rule.EvaluationReason
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.RuleEngine
import me.sensibile.augur.rule.json.RuleJson
import kotlin.test.Test
import kotlin.test.assertEquals

class RuleSdkEndToEndTest {
    @Test
    fun `decodes json builds sdk request and evaluates typed flag`() {
        val ruleSet =
            when (val result = RuleJson.decodeValidRuleSet(sampleRuleSetJson())) {
                is Outcome.Err -> error("Invalid test ruleset: ${result.error}")
                is Outcome.Ok -> result.value
            }
        val request =
            when (
                val result =
                    evaluationRequest(
                        flagKey = "new_checkout",
                        targetKey = "user-1",
                    ) {
                        string("country", "KR")
                        string("plan", "pro")
                    }
            ) {
                is Outcome.Err -> error("Invalid test request: ${result.error}")
                is Outcome.Ok -> result.value
            }

        val actual =
            RuleEngine.evaluateBoolean(
                ruleSet = ruleSet,
                request = request,
            )

        assertEquals(true, (actual as Outcome.Ok).value.value)
        assertEquals(EvaluationReason.RuleMatch, actual.value.decision.reason)
    }

    @Test
    fun `decodes json builds sdk request and evaluates typed default`() {
        val ruleSet =
            when (val result = RuleJson.decodeValidRuleSet(sampleRuleSetJson())) {
                is Outcome.Err -> error("Invalid test ruleset: ${result.error}")
                is Outcome.Ok -> result.value
            }
        val request =
            when (
                val result =
                    evaluationRequest(
                        flagKey = "new_checkout",
                        targetKey = "user-1",
                    ) {
                        string("country", "US")
                        string("plan", "free")
                    }
            ) {
                is Outcome.Err -> error("Invalid test request: ${result.error}")
                is Outcome.Ok -> result.value
            }

        val actual =
            RuleEngine.evaluateBoolean(
                ruleSet = ruleSet,
                request = request,
            )

        assertEquals(false, (actual as Outcome.Ok).value.value)
        assertEquals(EvaluationReason.Default, actual.value.decision.reason)
    }

    private fun sampleRuleSetJson(): String =
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
                  "condition": {
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
                }
              ]
            }
          ]
        }
        """.trimIndent()
}
