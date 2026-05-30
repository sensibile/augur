@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package me.sensibile.augur.rule.json

import me.sensibile.augur.rule.AttributeKey
import me.sensibile.augur.rule.Condition
import me.sensibile.augur.rule.Flag
import me.sensibile.augur.rule.FlagKey
import me.sensibile.augur.rule.Operator
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.Rule
import me.sensibile.augur.rule.RuleId
import me.sensibile.augur.rule.RuleSet
import me.sensibile.augur.rule.RuleSetSnapshot
import me.sensibile.augur.rule.RuleSetVersion
import me.sensibile.augur.rule.RuleValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.uuid.Uuid

class RuleJsonTest {
    @Test
    fun `encodes and decodes ruleset`() {
        val ruleSet =
            RuleSet(
                version = version(1),
                flags =
                    mapOf(
                        flagKey("new_checkout") to
                            Flag(
                                key = flagKey("new_checkout"),
                                enabled = true,
                                defaultValue = bool(false),
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
                                                            value = string("KR"),
                                                        ),
                                                        Condition.Predicate(
                                                            attributeKey = attributeKey("plan"),
                                                            operator = Operator.In,
                                                            value = list(string("pro"), string("team")),
                                                        ),
                                                    ),
                                                ),
                                            serve = bool(true),
                                        ),
                                    ),
                            ),
                    ),
            )

        val encoded = RuleJson.encodeRuleSet(ruleSet)
        val decoded = RuleJson.decodeRuleSet(encoded)

        assertEquals(Outcome.Ok(ruleSet), decoded)
    }

    @Test
    fun `returns value object error when json contains invalid flag key`() {
        val json =
            """
            {
              "version": 1,
              "flags": [
                {
                  "key": "Invalid Key",
                  "enabled": true,
                  "defaultValue": false,
                  "rules": []
                }
              ]
            }
            """.trimIndent()

        val actual = RuleJson.decodeRuleSet(json)

        assertIs<Outcome.Err<RuleJsonError.InvalidValueObject>>(actual)
    }

    @Test
    fun `decodes predicate condition`() {
        val json = ruleSetJson(predicateJson(field = "country", op = "Eq", value = """"KR""""))

        val actual = RuleJson.decodeRuleSet(json)

        assertIs<Outcome.Ok<RuleSet>>(actual)
    }

    @Test
    fun `decodes nested all any not conditions`() {
        val json =
            ruleSetJson(
                """
                {
                  "type": "all",
                  "conditions": [
                    ${predicateJson(field = "country", op = "Eq", value = """"KR"""")},
                    {
                      "type": "any",
                      "conditions": [
                        ${predicateJson(field = "plan", op = "In", value = """["pro", "team"]""")},
                        {
                          "type": "not",
                          "condition": ${predicateJson(field = "blocked", op = "Eq", value = "true")}
                        }
                      ]
                    }
                  ]
                }
                """.trimIndent(),
            )

        val actual = RuleJson.decodeRuleSet(json)

        assertIs<Outcome.Ok<RuleSet>>(actual)
    }

    @Test
    fun `decodes primitive and list rule values`() {
        val json =
            ruleSetJson(
                condition = predicateJson(field = "score", op = "GreaterThan", value = "10.5"),
                defaultValue = "null",
                serve = """["control", 1, true, null]""",
            )

        val actual = RuleJson.decodeRuleSet(json)

        assertIs<Outcome.Ok<RuleSet>>(actual)
    }

    @Test
    fun `returns invalid json when condition type is unknown`() {
        val json =
            ruleSetJson(
                """
                {
                  "type": "segment",
                  "name": "beta"
                }
                """.trimIndent(),
            )

        val actual = RuleJson.decodeRuleSet(json)

        assertIs<Outcome.Err<RuleJsonError.InvalidJson>>(actual)
    }

    @Test
    fun `returns invalid json when uuid is malformed`() {
        val json = ruleSetJson(condition = predicateJson(), ruleId = "not-a-uuid")

        val actual = RuleJson.decodeRuleSet(json)

        assertIs<Outcome.Err<RuleJsonError.InvalidJson>>(actual)
    }

    @Test
    fun `returns value object error when uuid is not v7`() {
        val json = ruleSetJson(condition = predicateJson(), ruleId = "550e8400-e29b-41d4-a716-446655440000")

        val actual = RuleJson.decodeRuleSet(json)

        assertIs<Outcome.Err<RuleJsonError.InvalidValueObject>>(actual)
    }

    @Test
    fun `returns value object error when version is invalid`() {
        val json = ruleSetJson(condition = predicateJson(), version = 0)

        val actual = RuleJson.decodeRuleSet(json)

        assertIs<Outcome.Err<RuleJsonError.InvalidValueObject>>(actual)
    }

    @Test
    fun `decodes rule set snapshot when json passes validation`() {
        val json = ruleSetJson(condition = predicateJson())

        val actual = RuleJson.decodeRuleSetSnapshot(json)

        assertIs<Outcome.Ok<RuleSetSnapshot>>(actual)
    }

    @Test
    fun `returns invalid rule set snapshot when decoded json fails validation`() {
        val json = ruleSetJson(condition = predicateJson(op = "GreaterThan", value = """"19""""))

        val raw = RuleJson.decodeRuleSet(json)
        val valid = RuleJson.decodeRuleSetSnapshot(json)

        assertIs<Outcome.Ok<RuleSet>>(raw)
        assertIs<Outcome.Err<RuleJsonError.InvalidRuleSet>>(valid)
    }

    @Test
    fun `returns duplicate flag key error before flags are associated by key`() {
        val json =
            """
            {
              "version": 1,
              "flags": [
                {
                  "key": "new_checkout",
                  "enabled": true,
                  "defaultValue": false,
                  "rules": []
                },
                {
                  "key": "new_checkout",
                  "enabled": true,
                  "defaultValue": true,
                  "rules": []
                }
              ]
            }
            """.trimIndent()

        val actual = RuleJson.decodeRuleSet(json)

        assertEquals(
            Outcome.Err(RuleJsonError.DuplicateFlagKey(flagKey("new_checkout"))),
            actual,
        )
    }

    private fun bool(value: Boolean): RuleValue.BooleanValue = RuleValue.boolean(value)

    private fun string(value: String): RuleValue.StringValue = RuleValue.string(value)

    private fun list(vararg values: RuleValue): RuleValue.ListValue = RuleValue.list(values.toList())

    private fun ruleSetJson(
        condition: String,
        version: Long = 1,
        ruleId: String = "01890f2e-7cc3-7cc3-8c4f-123456789abc",
        defaultValue: String = "false",
        serve: String = "true",
    ): String =
        """
        {
          "version": $version,
          "flags": [
            {
              "key": "new_checkout",
              "enabled": true,
              "defaultValue": $defaultValue,
              "rules": [
                {
                  "id": "$ruleId",
                  "serve": $serve,
                  "condition": $condition
                }
              ]
            }
          ]
        }
        """.trimIndent()

    private fun predicateJson(
        field: String = "country",
        op: String = "Eq",
        value: String = """"KR"""",
    ): String =
        """
        {
          "type": "predicate",
          "field": "$field",
          "op": "$op",
          "value": $value
        }
        """.trimIndent()

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
