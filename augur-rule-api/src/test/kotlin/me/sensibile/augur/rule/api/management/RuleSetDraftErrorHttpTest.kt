package me.sensibile.augur.rule.api.management

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.post
import kotlin.test.Test
import kotlin.test.assertNotNull

@SpringBootTest
@AutoConfigureMockMvc
class RuleSetDraftErrorHttpTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `returns conflict for duplicate flag`() {
        val created = createDraft()
        val draftId = requireJsonString(created.response.contentAsString, "draftId")
        val flagAdded = addFlag(draftId, requireHeader(created, HttpHeaders.ETAG), ERROR_FLAG_JSON)

        mockMvc
            .post("/rule-set-drafts/$draftId/flags") {
                contentType = MediaType.APPLICATION_JSON
                accept = MediaType.APPLICATION_JSON
                header(HttpHeaders.IF_MATCH, requireHeader(flagAdded, HttpHeaders.ETAG))
                content = ERROR_FLAG_JSON
            }.andExpect {
                status { isConflict() }
                jsonPath("$.title") { value("Rule management command rejected") }
                jsonPath("$.code") { value("flag_already_exists") }
                jsonPath("$.detail") { value("Flag new_checkout already exists in draft $draftId.") }
            }
    }

    @Test
    fun `returns conflict when editing validated draft`() {
        val created = createDraft()
        val draftId = requireJsonString(created.response.contentAsString, "draftId")
        val flagAdded = addFlag(draftId, requireHeader(created, HttpHeaders.ETAG), ERROR_FLAG_JSON)
        val validated = validateDraft(draftId, requireHeader(flagAdded, HttpHeaders.ETAG))

        mockMvc
            .post("/rule-set-drafts/$draftId/flags") {
                contentType = MediaType.APPLICATION_JSON
                accept = MediaType.APPLICATION_JSON
                header(HttpHeaders.IF_MATCH, requireHeader(validated, HttpHeaders.ETAG))
                content = ERROR_SECOND_FLAG_JSON
            }.andExpect {
                status { isConflict() }
                jsonPath("$.title") { value("Rule management command rejected") }
                jsonPath("$.code") { value("draft_is_not_editable") }
                jsonPath("$.detail") { value("Rule set draft $draftId is Validated and cannot be edited.") }
            }
    }

    @Test
    fun `returns unprocessable entity for invalid draft validation`() {
        val created = createDraft()
        val draftId = requireJsonString(created.response.contentAsString, "draftId")
        val flagAdded = addFlag(draftId, requireHeader(created, HttpHeaders.ETAG), ERROR_INVALID_FLAG_JSON)

        mockMvc
            .post("/rule-set-drafts/$draftId/validate") {
                accept = MediaType.APPLICATION_JSON
                header(HttpHeaders.IF_MATCH, requireHeader(flagAdded, HttpHeaders.ETAG))
            }.andExpect {
                status { isUnprocessableContent() }
                jsonPath("$.title") { value("Rule management command rejected") }
                jsonPath("$.code") { value("invalid_rule_set_draft") }
                jsonPath("$.detail") { value("Rule set draft $draftId failed validation.") }
            }
    }

    @Test
    fun `returns bad request for invalid flag key path variable`() {
        val created = createDraft()
        val draftId = requireJsonString(created.response.contentAsString, "draftId")

        mockMvc
            .post("/rule-set-drafts/$draftId/flags/INVALID/rules") {
                contentType = MediaType.APPLICATION_JSON
                accept = MediaType.APPLICATION_JSON
                header(HttpHeaders.IF_MATCH, requireHeader(created, HttpHeaders.ETAG))
                content = ERROR_RULE_JSON
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.title") { value("Rule management request is invalid") }
                jsonPath("$.code") { value("invalid_flag_key") }
                jsonPath("$.detail") { value("Invalid value for flagKey.") }
            }
    }

    private fun createDraft(): MvcResult =
        mockMvc
            .post("/rule-set-drafts") {
                contentType = MediaType.APPLICATION_JSON
                accept = MediaType.APPLICATION_JSON
                content = """{"version":1}"""
            }.andExpect {
                status { isCreated() }
            }.andReturn()

    private fun addFlag(
        draftId: String,
        ifMatch: String,
        body: String,
    ): MvcResult =
        mockMvc
            .post("/rule-set-drafts/$draftId/flags") {
                contentType = MediaType.APPLICATION_JSON
                accept = MediaType.APPLICATION_JSON
                header(HttpHeaders.IF_MATCH, ifMatch)
                content = body
            }.andExpect {
                status { isOk() }
            }.andReturn()

    private fun validateDraft(
        draftId: String,
        ifMatch: String,
    ): MvcResult =
        mockMvc
            .post("/rule-set-drafts/$draftId/validate") {
                accept = MediaType.APPLICATION_JSON
                header(HttpHeaders.IF_MATCH, ifMatch)
            }.andExpect {
                status { isOk() }
            }.andReturn()
}

private fun requireJsonString(
    json: String,
    property: String,
): String {
    val match = Regex(""""$property"\s*:\s*"([^"]+)"""").find(json)
    assertNotNull(match, "Missing JSON property $property in $json")
    return match.groupValues[1]
}

private fun requireHeader(
    result: MvcResult,
    header: String,
): String {
    val value = result.response.getHeader(header)
    assertNotNull(value, "Missing header $header")
    return value
}

private val ERROR_FLAG_JSON: String =
    """
    {
      "key": "new_checkout",
      "enabled": true,
      "defaultValue": false,
      "rules": []
    }
    """.trimIndent()

private val ERROR_SECOND_FLAG_JSON: String =
    """
    {
      "key": "search_ranking",
      "enabled": true,
      "defaultValue": false,
      "rules": []
    }
    """.trimIndent()

private val ERROR_INVALID_FLAG_JSON: String =
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

private val ERROR_RULE_JSON: String =
    """
    {
      "id": "018ff7c1-9354-7b02-b021-76d2791d6a24",
      "condition": {
        "type": "predicate",
        "field": "country",
        "op": "Eq",
        "value": "KR"
      },
      "serve": true
    }
    """.trimIndent()
