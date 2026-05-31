package me.sensibile.augur.rule.sdk

import me.sensibile.augur.rule.EvaluationError
import me.sensibile.augur.rule.EvaluationReason
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.RuleValueType
import me.sensibile.augur.rule.ValueObjectError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AugurEvaluatorTest {
    @Test
    fun `evaluates boolean flag from rule set snapshot`() {
        val evaluator = AugurEvaluator.of(validCheckoutRuleSet())

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
        val evaluator = AugurEvaluator.of(validCheckoutRuleSet())

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
        val evaluator = AugurEvaluator.of(validCheckoutRuleSet())

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
}
