package me.sensibile.augur.rule.sdk

import me.sensibile.augur.rule.AttributeKey
import me.sensibile.augur.rule.Condition
import me.sensibile.augur.rule.EvaluationError
import me.sensibile.augur.rule.EvaluationReason
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
import me.sensibile.augur.rule.RuleValueType
import me.sensibile.augur.rule.ValueObjectError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class AugurEvaluatorTest {
    @Test
    fun `evaluates boolean flag from rule set snapshot`() {
        val evaluator = AugurEvaluator.of(validRuleSet())

        val actual =
            evaluator.evaluateBoolean(
                flagKey = "new_checkout",
                targetKey = "user-1",
            ) {
                string("country", "KR")
                string("plan", "pro")
            }

        assertEquals(true, (actual as Outcome.Ok).value.value)
        assertEquals(EvaluationReason.RuleMatch, actual.value.decision.reason)
    }

    @Test
    fun `evaluates typed default from rule set snapshot`() {
        val evaluator = AugurEvaluator.of(validRuleSet())

        val actual =
            evaluator.evaluateBoolean(
                flagKey = "new_checkout",
                targetKey = "user-1",
            ) {
                string("country", "US")
                string("plan", "free")
            }

        assertEquals(false, (actual as Outcome.Ok).value.value)
        assertEquals(EvaluationReason.Default, actual.value.decision.reason)
    }

    @Test
    fun `returns invalid request error`() {
        val evaluator = AugurEvaluator.of(validRuleSet())

        val actual =
            evaluator.evaluateBoolean(
                flagKey = "Invalid Key",
                targetKey = "user-1",
            )

        assertEquals(
            AugurEvaluationError.InvalidRequest(
                EvaluationRequestBuildError.InvalidValueObject(
                    ValueObjectError.InvalidFormat("flagKey", "Invalid Key"),
                ),
            ),
            (actual as Outcome.Err).error,
        )
    }

    @Test
    fun `returns evaluation error when typed value differs`() {
        val evaluator = AugurEvaluator.of(validStringRuleSet())

        val actual =
            evaluator.evaluateBoolean(
                flagKey = "copy_text",
                targetKey = "user-1",
            )

        val error = ((actual as Outcome.Err).error as AugurEvaluationError.EvaluationFailed).error
        val unexpectedValueType = assertIs<EvaluationError.UnexpectedValueType>(error)
        assertEquals(flagKey("copy_text"), unexpectedValueType.flagKey)
        assertEquals(RuleValueType.Boolean, unexpectedValueType.expected)
        assertEquals(RuleValueType.String, unexpectedValueType.actual)
        assertEquals(EvaluationReason.Default, unexpectedValueType.decision.reason)
    }

    private fun validRuleSet() =
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

    private fun validStringRuleSet() =
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

    private fun valid(ruleSet: RuleSet) =
        when (val result = RuleSetValidator.validate(ruleSet)) {
            is Outcome.Err -> error("Invalid test ruleset: ${result.error}")
            is Outcome.Ok -> result.value
        }

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
}
