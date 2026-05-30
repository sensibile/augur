package me.sensibile.augur.rule

import me.sensibile.augur.rule.RuleFixtures.attributeKey
import me.sensibile.augur.rule.RuleFixtures.bool
import me.sensibile.augur.rule.RuleFixtures.flag
import me.sensibile.augur.rule.RuleFixtures.flagKey
import me.sensibile.augur.rule.RuleFixtures.number
import me.sensibile.augur.rule.RuleFixtures.predicate
import me.sensibile.augur.rule.RuleFixtures.rule
import me.sensibile.augur.rule.RuleFixtures.ruleId
import me.sensibile.augur.rule.RuleFixtures.ruleSet
import me.sensibile.augur.rule.RuleFixtures.string
import me.sensibile.augur.rule.RuleFixtures.version
import kotlin.test.Test
import kotlin.test.assertEquals

class RuleSetValidatorTest {
    @Test
    fun `returns valid ruleset when structure is valid`() {
        val ruleSet =
            ruleSet(
                flag(
                    key = flagKey("new_checkout"),
                    rules =
                        listOf(
                            rule(
                                condition =
                                    Condition.All(
                                        listOf(
                                            predicate("country", Operator.Eq, string("KR")),
                                            predicate("age", Operator.GreaterThanOrEqual, number(19.0)),
                                        ),
                                    ),
                            ),
                        ),
                ),
            )

        val actual = RuleSetValidator.validate(ruleSet)

        assertEquals(Outcome.Ok(ValidRuleSet(ruleSet)), actual)
    }

    @Test
    fun `detects flag key mismatch`() {
        val mapKey = flagKey("map_key")
        val flagKey = flagKey("flag_key")
        val ruleSet =
            RuleSet(
                version = version(1),
                flags =
                    mapOf(
                        mapKey to flag(key = flagKey),
                    ),
            )

        val actual = RuleSetValidator.validate(ruleSet)

        assertEquals(
            Outcome.Err(
                RuleSetValidationError(
                    listOf(RuleSetViolation.FlagKeyMismatch(mapKey, flagKey)),
                ),
            ),
            actual,
        )
    }

    @Test
    fun `detects duplicate rule id inside a flag`() {
        val ruleId = ruleId("01890f2e-7cc3-7cc3-8c4f-123456789abc")
        val ruleSet =
            ruleSet(
                flag(
                    key = flagKey("duplicated"),
                    rules =
                        listOf(
                            rule(id = ruleId),
                            rule(id = ruleId),
                        ),
                ),
            )

        val actual = RuleSetValidator.validate(ruleSet)

        assertEquals(
            Outcome.Err(
                RuleSetValidationError(
                    listOf(RuleSetViolation.DuplicateRuleId(flagKey("duplicated"), ruleId)),
                ),
            ),
            actual,
        )
    }

    @Test
    fun `detects empty all condition`() {
        val ruleId = ruleId("01890f2e-7cc3-7cc3-8c4f-123456789abc")
        val ruleSet =
            ruleSet(
                flag(
                    key = flagKey("empty_condition"),
                    rules =
                        listOf(
                            rule(
                                id = ruleId,
                                condition = Condition.All(emptyList()),
                            ),
                        ),
                ),
            )

        val actual = RuleSetValidator.validate(ruleSet)

        assertEquals(
            Outcome.Err(
                RuleSetValidationError(
                    listOf(
                        RuleSetViolation.EmptyConditionBranch(
                            flagKey = flagKey("empty_condition"),
                            ruleId = ruleId,
                            branchKind = BranchKind.All,
                        ),
                    ),
                ),
            ),
            actual,
        )
    }

    @Test
    fun `detects invalid operator value type`() {
        val ruleId = ruleId("01890f2e-7cc3-7cc3-8c4f-123456789abc")
        val age = attributeKey("age")
        val ruleSet =
            ruleSet(
                flag(
                    key = flagKey("age_gate"),
                    rules =
                        listOf(
                            rule(
                                id = ruleId,
                                condition = Condition.Predicate(age, Operator.GreaterThan, string("19")),
                            ),
                        ),
                ),
            )

        val actual = RuleSetValidator.validate(ruleSet)

        assertEquals(
            Outcome.Err(
                RuleSetValidationError(
                    listOf(
                        RuleSetViolation.InvalidPredicateValue(
                            flagKey = flagKey("age_gate"),
                            ruleId = ruleId,
                            attributeKey = age,
                            operator = Operator.GreaterThan,
                            expected = RuleValueExpectation.Number,
                            actual = RuleValueType.String,
                        ),
                    ),
                ),
            ),
            actual,
        )
    }
}
