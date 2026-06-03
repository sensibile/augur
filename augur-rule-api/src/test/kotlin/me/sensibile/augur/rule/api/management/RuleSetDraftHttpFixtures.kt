package me.sensibile.augur.rule.api.management

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.post
import kotlin.test.assertNotNull

internal fun MockMvc.createDraft(): MvcResult =
    post("/rule-set-drafts") {
        contentType = MediaType.APPLICATION_JSON
        accept = MediaType.APPLICATION_JSON
        content = """{"version":1}"""
    }.andExpect {
        status { isCreated() }
        jsonPath("$.eventType") { value("RuleSetDraftCreated") }
        jsonPath("$.draft.ruleSetVersion") { value(1) }
        jsonPath("$.draft.status") { value("Draft") }
        jsonPath("$.draft.flagCount") { value(0) }
        jsonPath("$.draft.streamVersion") { value(1) }
        header { string(HttpHeaders.ETAG, "\"1\"") }
    }.andReturn()

internal fun MockMvc.addFlag(
    draftId: String,
    ifMatch: String,
    body: String = FLAG_JSON,
): MvcResult =
    post("/rule-set-drafts/$draftId/flags") {
        contentType = MediaType.APPLICATION_JSON
        accept = MediaType.APPLICATION_JSON
        header(HttpHeaders.IF_MATCH, ifMatch)
        content = body
    }.andExpect {
        status { isOk() }
        jsonPath("$.eventType") { value("FlagAdded") }
        jsonPath("$.draft.flagCount") { value(1) }
        jsonPath("$.draft.streamVersion") { value(2) }
        header { string(HttpHeaders.ETAG, "\"2\"") }
    }.andReturn()

internal fun MockMvc.validateDraft(
    draftId: String,
    ifMatch: String,
): MvcResult =
    post("/rule-set-drafts/$draftId/validate") {
        accept = MediaType.APPLICATION_JSON
        header(HttpHeaders.IF_MATCH, ifMatch)
    }.andExpect {
        status { isOk() }
    }.andReturn()

internal fun MockMvc.postRule(
    draftId: String,
    ifMatch: String,
    body: String,
): ResultActionsDsl =
    post("/rule-set-drafts/$draftId/flags/new_checkout/rules") {
        contentType = MediaType.APPLICATION_JSON
        accept = MediaType.APPLICATION_JSON
        header(HttpHeaders.IF_MATCH, ifMatch)
        content = body
    }

internal fun requireJsonString(
    json: String,
    property: String,
): String {
    val match = Regex(""""$property"\s*:\s*"([^"]+)"""").find(json)
    assertNotNull(match, "Missing JSON property $property in $json")
    return match.groupValues[1]
}

internal fun requireHeader(
    result: MvcResult,
    header: String,
): String {
    val value = result.response.getHeader(header)
    assertNotNull(value, "Missing header $header")
    return value
}

internal val FLAG_JSON: String =
    """
    {
      "key": "new_checkout",
      "enabled": true,
      "defaultValue": false,
      "rules": []
    }
    """.trimIndent()

internal val SECOND_FLAG_JSON: String =
    """
    {
      "key": "search_ranking",
      "enabled": true,
      "defaultValue": false,
      "rules": []
    }
    """.trimIndent()

internal val RULE_JSON: String =
    """
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
    """.trimIndent()

internal val STRING_SERVE_RULE_JSON: String =
    """
    {
      "id": "018ff7c1-9354-7b02-b021-76d2791d6a22",
      "condition": {
        "type": "predicate",
        "field": "country",
        "op": "Eq",
        "value": "KR"
      },
      "serve": "enabled"
    }
    """.trimIndent()

internal val INVALID_FLAG_JSON: String =
    """
    {
      "key": "new_checkout",
      "enabled": true,
      "defaultValue": false,
      "rules": [
        {
          "id": "018ff7c1-9354-7b02-b021-76d2791d6a23",
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
    """.trimIndent()
