package me.sensibile.augur.rule.api.management

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import kotlin.test.Test
import kotlin.test.assertNotNull

@SpringBootTest
@AutoConfigureMockMvc
class RuleSetDraftHttpTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `creates edits validates and reads a rule set draft over http`() {
        val created =
            mockMvc
                .post("/rule-set-drafts") {
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
                }.andReturn()

        val draftId = requireJsonString(created.response.contentAsString, "draftId")

        mockMvc
            .post("/rule-set-drafts/$draftId/flags") {
                contentType = MediaType.APPLICATION_JSON
                accept = MediaType.APPLICATION_JSON
                content = flagJson()
            }.andExpect {
                status { isOk() }
                jsonPath("$.eventType") { value("FlagAdded") }
                jsonPath("$.draft.flagCount") { value(1) }
                jsonPath("$.draft.streamVersion") { value(2) }
            }

        mockMvc
            .post("/rule-set-drafts/$draftId/flags/new_checkout/rules") {
                contentType = MediaType.APPLICATION_JSON
                accept = MediaType.APPLICATION_JSON
                content = ruleJson()
            }.andExpect {
                status { isOk() }
                jsonPath("$.eventType") { value("RuleAdded") }
                jsonPath("$.draft.flagCount") { value(1) }
                jsonPath("$.draft.streamVersion") { value(3) }
            }

        mockMvc
            .post("/rule-set-drafts/$draftId/validate") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
                jsonPath("$.eventType") { value("RuleSetDraftValidated") }
                jsonPath("$.draft.status") { value("Validated") }
                jsonPath("$.draft.streamVersion") { value(4) }
            }

        mockMvc
            .get("/rule-set-drafts/$draftId") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
                jsonPath("$.draftId") { value(draftId) }
                jsonPath("$.status") { value("Validated") }
                jsonPath("$.flagCount") { value(1) }
                jsonPath("$.streamVersion") { value(4) }
            }
    }

    @Test
    fun `returns not found for missing draft`() {
        mockMvc
            .get("/rule-set-drafts/018ff7c1-9354-7b02-b021-76d2791d6a21") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("draft_not_found") }
            }
    }

    @Test
    fun `returns unprocessable entity for invalid flag json`() {
        val created =
            mockMvc
                .post("/rule-set-drafts") {
                    contentType = MediaType.APPLICATION_JSON
                    accept = MediaType.APPLICATION_JSON
                    content = """{"version":1}"""
                }.andReturn()
        val draftId = requireJsonString(created.response.contentAsString, "draftId")

        mockMvc
            .post("/rule-set-drafts/$draftId/flags") {
                contentType = MediaType.APPLICATION_JSON
                accept = MediaType.APPLICATION_JSON
                content = """{"key": ""}"""
            }.andExpect {
                status { isUnprocessableContent() }
                jsonPath("$.title") { value("Rule JSON is invalid") }
                jsonPath("$.code") { value("invalid_json") }
            }
    }

    @Test
    fun `returns unprocessable entity when rule serve type does not match flag default value`() {
        val created =
            mockMvc
                .post("/rule-set-drafts") {
                    contentType = MediaType.APPLICATION_JSON
                    accept = MediaType.APPLICATION_JSON
                    content = """{"version":1}"""
                }.andReturn()
        val draftId = requireJsonString(created.response.contentAsString, "draftId")

        mockMvc
            .post("/rule-set-drafts/$draftId/flags") {
                contentType = MediaType.APPLICATION_JSON
                accept = MediaType.APPLICATION_JSON
                content = flagJson()
            }.andExpect {
                status { isOk() }
            }

        mockMvc
            .post("/rule-set-drafts/$draftId/flags/new_checkout/rules") {
                contentType = MediaType.APPLICATION_JSON
                accept = MediaType.APPLICATION_JSON
                content = stringServeRuleJson()
            }.andExpect {
                status { isUnprocessableContent() }
                jsonPath("$.code") { value("serve_type_mismatch") }
            }
    }
}

private fun requireJsonString(
    json: String,
    property: String,
): String {
    val match = Regex(""""$property"\s*:\s*"([^"]+)"""").find(json)
    assertNotNull(match, "Missing JSON property $property in $json")
    return match.groupValues[1]
}

private fun flagJson(): String =
    """
    {
      "key": "new_checkout",
      "enabled": true,
      "defaultValue": false,
      "rules": []
    }
    """.trimIndent()

private fun ruleJson(): String =
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

private fun stringServeRuleJson(): String =
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
