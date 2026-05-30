package me.sensibile.augur.rule.sdk

import me.sensibile.augur.rule.AttributeKey
import me.sensibile.augur.rule.Condition
import me.sensibile.augur.rule.EvaluationReason
import me.sensibile.augur.rule.Flag
import me.sensibile.augur.rule.FlagKey
import me.sensibile.augur.rule.Operator
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.Rule
import me.sensibile.augur.rule.RuleEngine
import me.sensibile.augur.rule.RuleId
import me.sensibile.augur.rule.RuleSet
import me.sensibile.augur.rule.RuleSetValidator
import me.sensibile.augur.rule.RuleSetVersion
import me.sensibile.augur.rule.RuleValue
import me.sensibile.augur.rule.TargetKey
import me.sensibile.augur.rule.ValueObjectError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
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
        val ruleSet =
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
                                                    Condition.Predicate(
                                                        attributeKey = attributeKey("country"),
                                                        operator = Operator.Eq,
                                                        value = RuleValue.string("KR"),
                                                    ),
                                                serve = RuleValue.boolean(true),
                                            ),
                                        ),
                                ),
                        ),
                ),
            )
        val request =
            when (
                val result =
                    evaluationRequest(
                        flagKey = "new_checkout",
                        targetKey = "user-1",
                    ) {
                        string("country", "KR")
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

    private fun flagKey(value: String): FlagKey =
        when (val result = FlagKey.of(value)) {
            is Outcome.Err -> error("Invalid test flag key: $value")
            is Outcome.Ok -> result.value
        }

    private fun targetKey(value: String): TargetKey =
        when (val result = TargetKey.of(value)) {
            is Outcome.Err -> error("Invalid test target key: $value")
            is Outcome.Ok -> result.value
        }

    private fun attributeKey(value: String): AttributeKey =
        when (val result = AttributeKey.of(value)) {
            is Outcome.Err -> error("Invalid test attribute key: $value")
            is Outcome.Ok -> result.value
        }

    private fun number(value: Double): RuleValue.NumberValue =
        when (val result = RuleValue.number(value)) {
            is Outcome.Err -> error("Invalid test number value: $value")
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

    private fun valid(ruleSet: RuleSet) =
        when (val result = RuleSetValidator.validate(ruleSet)) {
            is Outcome.Err -> error("Invalid test ruleset: ${result.error}")
            is Outcome.Ok -> result.value
        }
}
