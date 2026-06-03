package me.sensibile.augur.rule.json

import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.RuleSet
import me.sensibile.augur.rule.RuleSetSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RuleJsonTest {
    @Test
    fun `encodes and decodes ruleset`() {
        val ruleSet = checkoutRuleSet()

        val encoded = RuleJson.encodeRuleSet(ruleSet)
        val decoded = RuleJson.decodeRuleSet(encoded)

        assertEquals(Outcome.Ok(ruleSet), decoded)
    }

    @Test
    fun `decodes a flag`() {
        val actual = RuleJson.decodeFlag(flagJson())

        assertIs<Outcome.Ok<*>>(actual)
    }

    @Test
    fun `decodes a rule`() {
        val actual = RuleJson.decodeRule(ruleJson())

        assertIs<Outcome.Ok<*>>(actual)
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
}
