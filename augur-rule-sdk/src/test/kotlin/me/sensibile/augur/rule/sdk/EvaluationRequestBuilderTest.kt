package me.sensibile.augur.rule.sdk

import me.sensibile.augur.rule.EvaluationReason
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.RuleEngine
import me.sensibile.augur.rule.RuleValue
import me.sensibile.augur.rule.ValueObjectError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class EvaluationRequestBuilderTest {
    @Test
    fun `builds evaluation request from primitive attributes`() {
        val actual =
            evaluationRequest(
                flagKey = "new_checkout",
                targetKey = "user-1",
            ) {
                string("country", "KR")
                number("age", 19.0)
                boolean("beta", true)
                nullValue("deleted_at")
            }

        assertEquals(
            flagKey("new_checkout"),
            (actual as Outcome.Ok).value.flagKey,
        )
        assertEquals(targetKey("user-1"), actual.value.context.targetKey)
        assertEquals(
            mapOf(
                attributeKey("country") to RuleValue.string("KR"),
                attributeKey("age") to number(19.0),
                attributeKey("beta") to RuleValue.boolean(true),
                attributeKey("deleted_at") to RuleValue.NullValue,
            ),
            actual.value.context.attributes,
        )
    }

    @Test
    fun `builds evaluation request from list attribute`() {
        val actual =
            evaluationRequest(
                flagKey = "new_checkout",
                targetKey = "user-1",
            ) {
                list("segments", listOf(RuleValue.string("beta"), RuleValue.string("paid")))
            }

        assertEquals(
            RuleValue.list(listOf(RuleValue.string("beta"), RuleValue.string("paid"))),
            (actual as Outcome.Ok).value.context.attributes[attributeKey("segments")],
        )
    }

    @Test
    fun `keeps first builder error when later attributes are also invalid`() {
        val actual =
            attributes {
                string("Invalid Key", "KR")
                number("score", Double.NaN)
            }

        assertEquals(
            Outcome.Err(
                EvaluationRequestBuildError.InvalidValueObject(
                    ValueObjectError.InvalidFormat("attributeKey", "Invalid Key"),
                ),
            ),
            actual,
        )
    }

    @Test
    fun `builds evaluation request from prebuilt attributes`() {
        val attributes =
            attributes {
                string("country", "KR")
            }
        val actual =
            evaluationRequest(
                flagKey = "new_checkout",
                targetKey = "user-1",
                attributes = (attributes as Outcome.Ok).value,
            )

        assertEquals(RuleValue.string("KR"), (actual as Outcome.Ok).value.context.attributes[attributeKey("country")])
    }

    @Test
    fun `returns value object error when flag key is invalid`() {
        val actual =
            evaluationRequest(
                flagKey = "Invalid Key",
                targetKey = "user-1",
            )

        assertIs<Outcome.Err<EvaluationRequestBuildError.InvalidValueObject>>(actual)
    }

    @Test
    fun `returns value object error when target key is invalid`() {
        val actual =
            evaluationRequest(
                flagKey = "new_checkout",
                targetKey = " ",
            )

        assertEquals(
            Outcome.Err(EvaluationRequestBuildError.InvalidValueObject(ValueObjectError.Blank("targetKey"))),
            actual,
        )
    }

    @Test
    fun `returns value object error when attribute key is invalid`() {
        val actual =
            attributes {
                string("Invalid Key", "KR")
            }

        assertIs<Outcome.Err<EvaluationRequestBuildError.InvalidValueObject>>(actual)
    }

    @Test
    fun `returns value object error when number attribute is not finite`() {
        val actual =
            attributes {
                number("score", Double.NaN)
            }

        assertEquals(
            Outcome.Err(EvaluationRequestBuildError.InvalidValueObject(ValueObjectError.NotFinite("ruleValue", Double.NaN))),
            actual,
        )
    }

    @Test
    fun `built request can be used with typed rule evaluation`() {
        val ruleSet = validCheckoutRuleSet()
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
}
