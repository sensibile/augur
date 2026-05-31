package me.sensibile.augur.rule.json

import me.sensibile.augur.rule.EvaluationReason
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.RuleEngine
import me.sensibile.augur.rule.RuleValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RuleJsonEngineIntegrationTest {
    @Test
    fun `decodes valid json and evaluates matched rule`() {
        val ruleSet =
            when (val decoded = RuleJson.decodeRuleSetSnapshot(sampleRuleSetJson())) {
                is Outcome.Err -> error("Expected rule set snapshot: ${decoded.error}")
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
            when (val decoded = RuleJson.decodeRuleSetSnapshot(sampleRuleSetJson())) {
                is Outcome.Err -> error("Expected rule set snapshot: ${decoded.error}")
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

        val decoded = RuleJson.decodeRuleSetSnapshot(json)

        assertIs<Outcome.Err<RuleJsonError.InvalidRuleSet>>(decoded)
    }
}
