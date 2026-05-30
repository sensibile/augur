package me.sensibile.augur.rule

import me.sensibile.augur.rule.RuleFixtures.attributeKey
import me.sensibile.augur.rule.RuleFixtures.bool
import me.sensibile.augur.rule.RuleFixtures.flag
import me.sensibile.augur.rule.RuleFixtures.flagKey
import me.sensibile.augur.rule.RuleFixtures.request
import me.sensibile.augur.rule.RuleFixtures.rule
import me.sensibile.augur.rule.RuleFixtures.ruleId
import me.sensibile.augur.rule.RuleFixtures.ruleSet
import me.sensibile.augur.rule.RuleFixtures.string
import kotlin.test.Test
import kotlin.test.assertEquals

class RuleEngineTest {
    @Test
    fun `validates raw ruleset before evaluation`() {
        val ruleId = ruleId("01890f2e-7cc3-7cc3-8c4f-123456789abc")
        val ruleSet =
            ruleSet(
                flag(
                    key = flagKey("age_gate"),
                    rules =
                        listOf(
                            rule(
                                id = ruleId,
                                condition =
                                    Condition.Predicate(
                                        attributeKey = attributeKey("age"),
                                        operator = Operator.GreaterThanOrEqual,
                                        value = string("19"),
                                    ),
                            ),
                        ),
                ),
            )

        val actual =
            RuleEngine.evaluate(
                ruleSet = ruleSet,
                request = request("age_gate"),
            )

        assertEquals(
            Outcome.Err(
                EvaluationError.InvalidRuleSet(
                    RuleSetValidationError(
                        listOf(
                            RuleSetViolation.InvalidPredicateValue(
                                flagKey = flagKey("age_gate"),
                                ruleId = ruleId,
                                attributeKey = attributeKey("age"),
                                operator = Operator.GreaterThanOrEqual,
                                expected = RuleValueExpectation.Number,
                                actual = RuleValueType.String,
                            ),
                        ),
                    ),
                ),
            ),
            actual,
        )
    }

    @Test
    fun `evaluates raw ruleset when validation passes`() {
        val ruleSet =
            ruleSet(
                flag(
                    key = flagKey("new_checkout"),
                    rules =
                        listOf(
                            rule(
                                condition = Condition.Predicate(attributeKey("country"), Operator.Eq, string("KR")),
                            ),
                        ),
                ),
            )

        val actual =
            RuleEngine.evaluate(
                ruleSet = ruleSet,
                request =
                    request(
                        flagKey = "new_checkout",
                        attributes = mapOf(attributeKey("country") to string("KR")),
                    ),
            )

        assertEquals(EvaluationReason.RuleMatch, (actual as Outcome.Ok).value.reason)
    }
}
