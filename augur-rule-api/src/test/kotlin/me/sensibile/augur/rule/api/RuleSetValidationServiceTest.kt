package me.sensibile.augur.rule.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RuleSetValidationServiceTest {
    private val service = RuleSetValidationService()

    @Test
    fun `validates canonical rule set json`() {
        val result = service.validateCanonicalJson(validRuleSetJson())

        val valid = assertIs<RuleSetValidationResult.Valid>(result)
        assertEquals(RuleSetSnapshotSummary(version = 1, flagCount = 1), valid.summary)
    }

    @Test
    fun `returns validation errors for invalid snapshots`() {
        val result = service.validateCanonicalJson(invalidServeTypeRuleSetJson())

        val invalid = assertIs<RuleSetValidationResult.Invalid>(result)
        assertEquals("invalid_rule_set", invalid.error.code)
        assertEquals(1, invalid.error.violations.size)
    }

    @Test
    fun `returns json errors for malformed input`() {
        val result = service.validateCanonicalJson("""{"version":1""")

        val invalid = assertIs<RuleSetValidationResult.Invalid>(result)
        assertEquals("invalid_json", invalid.error.code)
    }
}

internal fun validRuleSetJson(): String =
    """
    {
      "version": 1,
      "flags": [
        {
          "key": "new_checkout",
          "enabled": true,
          "defaultValue": false,
          "rules": [
            {
              "id": "018ff7c1-9354-7b02-b021-76d2791d6a21",
              "condition": {
                "type": "predicate",
                "field": "country",
                "op": "Eq",
                "value": "KR"
              },
              "serve": true
            }
          ]
        }
      ]
    }
    """.trimIndent()

internal fun invalidServeTypeRuleSetJson(): String =
    """
    {
      "version": 1,
      "flags": [
        {
          "key": "new_checkout",
          "enabled": true,
          "defaultValue": false,
          "rules": [
            {
              "id": "018ff7c1-9354-7b02-b021-76d2791d6a21",
              "condition": {
                "type": "predicate",
                "field": "country",
                "op": "Eq",
                "value": "KR"
              },
              "serve": "enabled"
            }
          ]
        }
      ]
    }
    """.trimIndent()
