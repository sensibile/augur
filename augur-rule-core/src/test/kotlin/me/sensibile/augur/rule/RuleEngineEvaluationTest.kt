package me.sensibile.augur.rule

import me.sensibile.augur.rule.RuleFixtures.attributeKey
import me.sensibile.augur.rule.RuleFixtures.bool
import me.sensibile.augur.rule.RuleFixtures.flag
import me.sensibile.augur.rule.RuleFixtures.flagKey
import me.sensibile.augur.rule.RuleFixtures.list
import me.sensibile.augur.rule.RuleFixtures.number
import me.sensibile.augur.rule.RuleFixtures.predicate
import me.sensibile.augur.rule.RuleFixtures.request
import me.sensibile.augur.rule.RuleFixtures.rule
import me.sensibile.augur.rule.RuleFixtures.ruleId
import me.sensibile.augur.rule.RuleFixtures.ruleSet
import me.sensibile.augur.rule.RuleFixtures.string
import me.sensibile.augur.rule.RuleFixtures.valid
import me.sensibile.augur.rule.RuleFixtures.version
import kotlin.test.Test
import kotlin.test.assertEquals

class RuleEngineEvaluationTest {
    @Test
    fun `returns matched rule value by rule order`() {
        val matchedRuleId = ruleId("01890f2e-7cc3-7cc3-8c4f-123456789abc")
        val ruleSet =
            ruleSet(
                flag(
                    key = flagKey("new_checkout"),
                    defaultValue = bool(false),
                    rules =
                        listOf(
                            rule(
                                id = matchedRuleId,
                                condition = predicate("country", Operator.Eq, string("KR")),
                                serve = bool(true),
                            ),
                            rule(
                                id = ruleId("01890f2e-7cc3-7cc3-8c4f-123456789abd"),
                                condition = predicate("country", Operator.Eq, string("US")),
                                serve = bool(false),
                            ),
                        ),
                ),
            )

        val actual =
            RuleEngine.evaluate(
                ruleSet = valid(ruleSet),
                request =
                    request(
                        flagKey = "new_checkout",
                        attributes = mapOf(attributeKey("country") to string("KR")),
                    ),
            )

        assertEquals(
            Outcome.Ok(
                EvaluationDecision(
                    flagKey = flagKey("new_checkout"),
                    value = bool(true),
                    reason = EvaluationReason.RuleMatch,
                    matchedRuleId = matchedRuleId,
                    ruleSetVersion = version(1),
                    trace =
                        EvaluationTrace(
                            listOf(RuleEvaluationTrace(matchedRuleId, matched = true)),
                        ),
                ),
            ),
            actual,
        )
    }

    @Test
    fun `returns default when no rule matches`() {
        val unmatchedRuleId = ruleId("01890f2e-7cc3-7cc3-8c4f-123456789abc")
        val ruleSet =
            ruleSet(
                flag(
                    key = flagKey("new_checkout"),
                    defaultValue = bool(false),
                    rules =
                        listOf(
                            rule(
                                id = unmatchedRuleId,
                                condition = predicate("country", Operator.Eq, string("KR")),
                                serve = bool(true),
                            ),
                        ),
                ),
            )

        val actual =
            RuleEngine.evaluate(
                ruleSet = valid(ruleSet),
                request =
                    request(
                        flagKey = "new_checkout",
                        attributes = mapOf(attributeKey("country") to string("US")),
                    ),
            )

        assertEquals(
            Outcome.Ok(
                EvaluationDecision(
                    flagKey = flagKey("new_checkout"),
                    value = bool(false),
                    reason = EvaluationReason.Default,
                    ruleSetVersion = version(1),
                    trace =
                        EvaluationTrace(
                            listOf(RuleEvaluationTrace(unmatchedRuleId, matched = false)),
                        ),
                ),
            ),
            actual,
        )
    }

    @Test
    fun `returns default when type mismatches`() {
        val ruleSet =
            ruleSet(
                flag(
                    key = flagKey("age_gate"),
                    defaultValue = bool(false),
                    rules =
                        listOf(
                            rule(
                                condition = predicate("age", Operator.GreaterThanOrEqual, number(19.0)),
                                serve = bool(true),
                            ),
                        ),
                ),
            )

        val actual =
            RuleEngine.evaluate(
                ruleSet = valid(ruleSet),
                request =
                    request(
                        flagKey = "age_gate",
                        attributes = mapOf(attributeKey("age") to string("twenty")),
                    ),
            )

        assertEquals(EvaluationReason.Default, (actual as Outcome.Ok).value.reason)
    }

    @Test
    fun `evaluates nested boolean conditions`() {
        val condition =
            Condition.All(
                listOf(
                    predicate("country", Operator.Eq, string("KR")),
                    Condition.Any(
                        listOf(
                            predicate("plan", Operator.In, list(string("pro"), string("team"))),
                            Condition.Not(predicate("blocked", Operator.Eq, bool(true))),
                        ),
                    ),
                ),
            )
        val ruleSet =
            ruleSet(
                flag(
                    key = flagKey("advanced_rule"),
                    defaultValue = bool(false),
                    rules = listOf(rule(condition = condition, serve = bool(true))),
                ),
            )

        val actual =
            RuleEngine.evaluate(
                ruleSet = valid(ruleSet),
                request =
                    request(
                        flagKey = "advanced_rule",
                        attributes =
                            mapOf(
                                attributeKey("country") to string("KR"),
                                attributeKey("plan") to string("free"),
                                attributeKey("blocked") to bool(false),
                            ),
                    ),
            )

        assertEquals(EvaluationReason.RuleMatch, (actual as Outcome.Ok).value.reason)
    }

    @Test
    fun `trace only includes rules evaluated before first match`() {
        val firstRuleId = ruleId("01890f2e-7cc3-7cc3-8c4f-123456789abc")
        val secondRuleId = ruleId("01890f2e-7cc3-7cc3-8c4f-123456789abd")
        val thirdRuleId = ruleId("01890f2e-7cc3-7cc3-8c4f-123456789abe")
        val ruleSet =
            ruleSet(
                flag(
                    key = flagKey("ordered_rules"),
                    defaultValue = bool(false),
                    rules =
                        listOf(
                            rule(
                                id = firstRuleId,
                                condition = predicate("country", Operator.Eq, string("US")),
                                serve = bool(false),
                            ),
                            rule(
                                id = secondRuleId,
                                condition = predicate("country", Operator.Eq, string("KR")),
                                serve = bool(true),
                            ),
                            rule(
                                id = thirdRuleId,
                                condition = predicate("country", Operator.Exists),
                                serve = bool(false),
                            ),
                        ),
                ),
            )

        val actual =
            RuleEngine.evaluate(
                ruleSet = valid(ruleSet),
                request =
                    request(
                        flagKey = "ordered_rules",
                        attributes = mapOf(attributeKey("country") to string("KR")),
                    ),
            )

        assertEquals(
            EvaluationTrace(
                listOf(
                    RuleEvaluationTrace(firstRuleId, matched = false),
                    RuleEvaluationTrace(secondRuleId, matched = true),
                ),
            ),
            (actual as Outcome.Ok).value.trace,
        )
    }

    @Test
    fun `trace exposes evaluated matched and missed rules`() {
        val missedRuleId = ruleId("01890f2e-7cc3-7cc3-8c4f-123456789abc")
        val matchedRuleId = ruleId("01890f2e-7cc3-7cc3-8c4f-123456789abd")
        val trace =
            EvaluationTrace(
                listOf(
                    RuleEvaluationTrace(missedRuleId, matched = false),
                    RuleEvaluationTrace(matchedRuleId, matched = true),
                ),
            )

        assertEquals(listOf(missedRuleId, matchedRuleId), trace.evaluatedRuleIds)
        assertEquals(matchedRuleId, trace.matchedRuleId)
        assertEquals(listOf(RuleEvaluationTrace(matchedRuleId, matched = true)), trace.matched)
        assertEquals(listOf(RuleEvaluationTrace(missedRuleId, matched = false)), trace.missed)
    }

    @Test
    fun `trace exposes no matched rule when all rules miss`() {
        val missedRuleId = ruleId("01890f2e-7cc3-7cc3-8c4f-123456789abc")
        val trace =
            EvaluationTrace(
                listOf(
                    RuleEvaluationTrace(missedRuleId, matched = false),
                ),
            )

        assertEquals(null, trace.matchedRuleId)
        assertEquals(emptyList(), trace.matched)
        assertEquals(listOf(RuleEvaluationTrace(missedRuleId, matched = false)), trace.missed)
    }

    @Test
    fun `evaluates not condition`() {
        val actual =
            evaluateSingleRule(
                condition = Condition.Not(predicate("blocked", Operator.Eq, bool(true))),
                attributes = mapOf(attributeKey("blocked") to bool(false)),
            )

        assertEquals(EvaluationReason.RuleMatch, actual.reason)
    }

    @Test
    fun `not condition misses when nested condition matches`() {
        val actual =
            evaluateSingleRule(
                condition = Condition.Not(predicate("blocked", Operator.Eq, bool(true))),
                attributes = mapOf(attributeKey("blocked") to bool(true)),
            )

        assertEquals(EvaluationReason.Default, actual.reason)
    }

    @Test
    fun `evaluates presence operators`() {
        val existsDecision =
            evaluateSingleRule(
                condition = predicate("country", Operator.Exists),
                attributes = mapOf(attributeKey("country") to string("KR")),
            )
        val missingDecision =
            evaluateSingleRule(
                condition = predicate("country", Operator.Missing),
            )

        assertEquals(EvaluationReason.RuleMatch, existsDecision.reason)
        assertEquals(EvaluationReason.RuleMatch, missingDecision.reason)
    }

    @Test
    fun `presence operators miss on opposite presence state`() {
        val existsDecision =
            evaluateSingleRule(
                condition = predicate("country", Operator.Exists),
            )
        val missingDecision =
            evaluateSingleRule(
                condition = predicate("country", Operator.Missing),
                attributes = mapOf(attributeKey("country") to string("KR")),
            )

        assertEquals(EvaluationReason.Default, existsDecision.reason)
        assertEquals(EvaluationReason.Default, missingDecision.reason)
    }

    @Test
    fun `evaluates negative equality and membership operators`() {
        val notEqDecision =
            evaluateSingleRule(
                condition = predicate("country", Operator.NotEq, string("US")),
                attributes = mapOf(attributeKey("country") to string("KR")),
            )
        val notInDecision =
            evaluateSingleRule(
                condition = predicate("plan", Operator.NotIn, list(string("free"), string("trial"))),
                attributes = mapOf(attributeKey("plan") to string("pro")),
            )

        assertEquals(EvaluationReason.RuleMatch, notEqDecision.reason)
        assertEquals(EvaluationReason.RuleMatch, notInDecision.reason)
    }

    @Test
    fun `negative equality and membership miss for missing or contained values`() {
        val notEqDecision =
            evaluateSingleRule(
                condition = predicate("country", Operator.NotEq, string("US")),
            )
        val notInDecision =
            evaluateSingleRule(
                condition = predicate("plan", Operator.NotIn, list(string("free"), string("trial"))),
                attributes = mapOf(attributeKey("plan") to string("free")),
            )

        assertEquals(EvaluationReason.Default, notEqDecision.reason)
        assertEquals(EvaluationReason.Default, notInDecision.reason)
    }

    @Test
    fun `evaluates numeric comparison operators`() {
        val greaterThanDecision =
            evaluateSingleRule(
                condition = predicate("age", Operator.GreaterThan, number(18.0)),
                attributes = mapOf(attributeKey("age") to number(19.0)),
            )
        val lessThanDecision =
            evaluateSingleRule(
                condition = predicate("age", Operator.LessThan, number(20.0)),
                attributes = mapOf(attributeKey("age") to number(19.0)),
            )
        val lessThanOrEqualDecision =
            evaluateSingleRule(
                condition = predicate("age", Operator.LessThanOrEqual, number(19.0)),
                attributes = mapOf(attributeKey("age") to number(19.0)),
            )

        assertEquals(EvaluationReason.RuleMatch, greaterThanDecision.reason)
        assertEquals(EvaluationReason.RuleMatch, lessThanDecision.reason)
        assertEquals(EvaluationReason.RuleMatch, lessThanOrEqualDecision.reason)
    }

    @Test
    fun `numeric comparison operators miss when comparison is false`() {
        val greaterThanDecision =
            evaluateSingleRule(
                condition = predicate("age", Operator.GreaterThan, number(18.0)),
                attributes = mapOf(attributeKey("age") to number(18.0)),
            )
        val greaterThanOrEqualDecision =
            evaluateSingleRule(
                condition = predicate("age", Operator.GreaterThanOrEqual, number(19.0)),
                attributes = mapOf(attributeKey("age") to number(18.0)),
            )
        val lessThanDecision =
            evaluateSingleRule(
                condition = predicate("age", Operator.LessThan, number(20.0)),
                attributes = mapOf(attributeKey("age") to number(20.0)),
            )
        val lessThanOrEqualDecision =
            evaluateSingleRule(
                condition = predicate("age", Operator.LessThanOrEqual, number(19.0)),
                attributes = mapOf(attributeKey("age") to number(20.0)),
            )

        assertEquals(EvaluationReason.Default, greaterThanDecision.reason)
        assertEquals(EvaluationReason.Default, greaterThanOrEqualDecision.reason)
        assertEquals(EvaluationReason.Default, lessThanDecision.reason)
        assertEquals(EvaluationReason.Default, lessThanOrEqualDecision.reason)
    }

    @Test
    fun `evaluates contains operators`() {
        val stringContainsDecision =
            evaluateSingleRule(
                condition = predicate("email", Operator.Contains, string("@example.com")),
                attributes = mapOf(attributeKey("email") to string("user@example.com")),
            )
        val listContainsDecision =
            evaluateSingleRule(
                condition = predicate("groups", Operator.Contains, string("beta")),
                attributes = mapOf(attributeKey("groups") to list(string("beta"), string("internal"))),
            )

        assertEquals(EvaluationReason.RuleMatch, stringContainsDecision.reason)
        assertEquals(EvaluationReason.RuleMatch, listContainsDecision.reason)
    }

    @Test
    fun `contains operator misses for unsupported actual value type`() {
        val actual =
            evaluateSingleRule(
                condition = predicate("email", Operator.Contains, string("@example.com")),
                attributes = mapOf(attributeKey("email") to bool(true)),
            )

        assertEquals(EvaluationReason.Default, actual.reason)
    }

    @Test
    fun `evaluates string prefix and suffix operators`() {
        val startsWithDecision =
            evaluateSingleRule(
                condition = predicate("email", Operator.StartsWith, string("user")),
                attributes = mapOf(attributeKey("email") to string("user@example.com")),
            )
        val endsWithDecision =
            evaluateSingleRule(
                condition = predicate("email", Operator.EndsWith, string("example.com")),
                attributes = mapOf(attributeKey("email") to string("user@example.com")),
            )

        assertEquals(EvaluationReason.RuleMatch, startsWithDecision.reason)
        assertEquals(EvaluationReason.RuleMatch, endsWithDecision.reason)
    }

    @Test
    fun `disabled flag returns default with disabled reason`() {
        val ruleSet =
            ruleSet(
                flag(
                    key = flagKey("kill_switch"),
                    enabled = false,
                    defaultValue = bool(false),
                    rules = listOf(rule(condition = predicate("country", Operator.Exists), serve = bool(true))),
                ),
            )

        val actual =
            RuleEngine.evaluate(
                ruleSet = valid(ruleSet),
                request =
                    request(
                        flagKey = "kill_switch",
                        attributes = mapOf(attributeKey("country") to string("KR")),
                    ),
            )

        assertEquals(EvaluationReason.FlagDisabled, (actual as Outcome.Ok).value.reason)
        assertEquals(bool(false), actual.value.value)
    }

    @Test
    fun `unknown flag returns null with not found reason`() {
        val actual =
            RuleEngine.evaluate(
                ruleSet = valid(ruleSet()),
                request = request(flagKey = "missing_flag"),
            )

        assertEquals(EvaluationReason.FlagNotFound, (actual as Outcome.Ok).value.reason)
        assertEquals(RuleValue.NullValue, actual.value.value)
    }

    private fun evaluateSingleRule(
        condition: Condition,
        attributes: Map<AttributeKey, RuleValue> = emptyMap(),
    ): EvaluationDecision {
        val ruleSet =
            ruleSet(
                flag(
                    key = flagKey("single_rule"),
                    defaultValue = bool(false),
                    rules = listOf(rule(condition = condition, serve = bool(true))),
                ),
            )
        val actual =
            RuleEngine.evaluate(
                ruleSet = valid(ruleSet),
                request =
                    request(
                        flagKey = "single_rule",
                        attributes = attributes,
                    ),
            )

        return (actual as Outcome.Ok).value
    }
}
