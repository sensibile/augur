package me.sensibile.augur.rule

import me.sensibile.augur.rule.RuleFixtures.attributeKey
import me.sensibile.augur.rule.RuleFixtures.bool
import me.sensibile.augur.rule.RuleFixtures.flag
import me.sensibile.augur.rule.RuleFixtures.flagKey
import me.sensibile.augur.rule.RuleFixtures.number
import me.sensibile.augur.rule.RuleFixtures.request
import me.sensibile.augur.rule.RuleFixtures.rule
import me.sensibile.augur.rule.RuleFixtures.ruleId
import me.sensibile.augur.rule.RuleFixtures.ruleSet
import me.sensibile.augur.rule.RuleFixtures.string
import me.sensibile.augur.rule.RuleFixtures.valid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RuleEngineTypedEvaluationTest {
    @Test
    fun `evaluates boolean flag with typed value and decision metadata`() {
        val ruleId = ruleId("01890f2e-7cc3-7cc3-8c4f-123456789abc")
        val ruleSet =
            ruleSet(
                flag(
                    key = flagKey("new_checkout"),
                    defaultValue = bool(false),
                    rules =
                        listOf(
                            rule(
                                id = ruleId,
                                condition = Condition.Predicate(attributeKey("country"), Operator.Eq, string("KR")),
                                serve = bool(true),
                            ),
                        ),
                ),
            )

        val actual =
            RuleEngine.evaluateBoolean(
                ruleSet = valid(ruleSet),
                request =
                    request(
                        flagKey = "new_checkout",
                        attributes = mapOf(attributeKey("country") to string("KR")),
                    ),
            )

        assertEquals(true, (actual as Outcome.Ok).value.value)
        assertEquals(EvaluationReason.RuleMatch, actual.value.decision.reason)
        assertEquals(ruleId, actual.value.decision.matchedRuleId)
    }

    @Test
    fun `returns type error when requested type differs from evaluated value`() {
        val ruleSet =
            ruleSet(
                flag(
                    key = flagKey("copy_text"),
                    defaultValue = string("control"),
                    rules =
                        listOf(
                            rule(
                                serve = string("variant"),
                            ),
                        ),
                ),
            )

        val actual =
            RuleEngine.evaluateBoolean(
                ruleSet = valid(ruleSet),
                request =
                    request(
                        flagKey = "copy_text",
                        attributes = mapOf(attributeKey("country") to string("KR")),
                    ),
            )

        assertEquals(
            EvaluationError.UnexpectedValueType(
                flagKey = flagKey("copy_text"),
                expected = RuleValueType.Boolean,
                actual = RuleValueType.String,
                decision =
                    EvaluationDecision(
                        flagKey = flagKey("copy_text"),
                        value = string("variant"),
                        reason = EvaluationReason.RuleMatch,
                        matchedRuleId = ruleId("01890f2e-7cc3-7cc3-8c4f-123456789abc"),
                        ruleSetVersion = RuleFixtures.version(1),
                        trace =
                            EvaluationTrace(
                                listOf(
                                    RuleEvaluationTrace(
                                        ruleId("01890f2e-7cc3-7cc3-8c4f-123456789abc"),
                                        matched = true,
                                    ),
                                ),
                            ),
                    ),
            ),
            (actual as Outcome.Err).error,
        )
    }

    @Test
    fun `typed raw evaluation returns invalid ruleset error before type coercion`() {
        val ruleId = ruleId("01890f2e-7cc3-7cc3-8c4f-123456789abc")
        val ruleSet =
            ruleSet(
                flag(
                    key = flagKey("age_gate"),
                    rules =
                        listOf(
                            rule(
                                id = ruleId,
                                condition = Condition.Predicate(attributeKey("age"), Operator.GreaterThan, string("19")),
                            ),
                        ),
                ),
            )

        val actual =
            RuleEngine.evaluateBoolean(
                ruleSet = ruleSet,
                request = request("age_gate"),
            )

        assertIs<Outcome.Err<EvaluationError.InvalidRuleSet>>(actual)
    }

    @Test
    fun `evaluates string number and list flags`() {
        val stringDecision =
            RuleEngine.evaluateString(
                ruleSet =
                    valid(
                        ruleSet(
                            flag(
                                key = flagKey("copy_text"),
                                defaultValue = string("control"),
                                rules = emptyList(),
                            ),
                        ),
                    ),
                request = request("copy_text"),
            )
        val numberDecision =
            RuleEngine.evaluateNumber(
                ruleSet =
                    valid(
                        ruleSet(
                            flag(
                                key = flagKey("discount_rate"),
                                defaultValue = number(0.1),
                                rules = emptyList(),
                            ),
                        ),
                    ),
                request = request("discount_rate"),
            )
        val listDecision =
            RuleEngine.evaluateList(
                ruleSet =
                    valid(
                        ruleSet(
                            flag(
                                key = flagKey("segments"),
                                defaultValue = RuleValue.list(listOf(string("beta"), string("internal"))),
                                rules = emptyList(),
                            ),
                        ),
                    ),
                request = request("segments"),
            )

        assertEquals("control", (stringDecision as Outcome.Ok).value.value)
        assertEquals(0.1, (numberDecision as Outcome.Ok).value.value)
        assertEquals(listOf(string("beta"), string("internal")), (listDecision as Outcome.Ok).value.value)
    }
}
