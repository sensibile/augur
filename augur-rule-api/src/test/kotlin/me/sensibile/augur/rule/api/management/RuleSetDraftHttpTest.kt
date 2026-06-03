package me.sensibile.augur.rule.api.management

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import kotlin.test.Test

@SpringBootTest
@AutoConfigureMockMvc
class RuleSetDraftHttpTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `creates edits validates and reads a rule set draft over http`() {
        val created = mockMvc.createDraft()
        val draftId = requireJsonString(created.response.contentAsString, "draftId")
        val flagAdded = mockMvc.addFlag(draftId, requireHeader(created, HttpHeaders.ETAG))
        val ruleAdded =
            mockMvc
                .postRule(draftId, requireHeader(flagAdded, HttpHeaders.ETAG), RULE_JSON)
                .andExpect {
                    status { isOk() }
                    jsonPath("$.eventType") { value("RuleAdded") }
                    jsonPath("$.draft.flagCount") { value(1) }
                    jsonPath("$.draft.streamVersion") { value(3) }
                    header { string(HttpHeaders.ETAG, "\"3\"") }
                }.andReturn()

        mockMvc
            .post("/rule-set-drafts/$draftId/validate") {
                accept = MediaType.APPLICATION_JSON
                header(HttpHeaders.IF_MATCH, requireHeader(ruleAdded, HttpHeaders.ETAG))
            }.andExpect {
                status { isOk() }
                jsonPath("$.eventType") { value("RuleSetDraftValidated") }
                jsonPath("$.draft.status") { value("Validated") }
                jsonPath("$.draft.streamVersion") { value(4) }
                header { string(HttpHeaders.ETAG, "\"4\"") }
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
                header { string(HttpHeaders.ETAG, "\"4\"") }
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
        val created = mockMvc.createDraft()
        val draftId = requireJsonString(created.response.contentAsString, "draftId")
        val createdETag = requireHeader(created, HttpHeaders.ETAG)

        mockMvc
            .post("/rule-set-drafts/$draftId/flags") {
                contentType = MediaType.APPLICATION_JSON
                accept = MediaType.APPLICATION_JSON
                header(HttpHeaders.IF_MATCH, createdETag)
                content = """{"key": ""}"""
            }.andExpect {
                status { isUnprocessableContent() }
                jsonPath("$.title") { value("Rule JSON is invalid") }
                jsonPath("$.code") { value("invalid_json") }
            }
    }

    @Test
    fun `returns unprocessable entity when rule serve type does not match flag default value`() {
        val created = mockMvc.createDraft()
        val draftId = requireJsonString(created.response.contentAsString, "draftId")
        val createdETag = requireHeader(created, HttpHeaders.ETAG)

        val flagAdded = mockMvc.addFlag(draftId, createdETag)

        mockMvc
            .postRule(draftId, requireHeader(flagAdded, HttpHeaders.ETAG), STRING_SERVE_RULE_JSON)
            .andExpect {
                status { isUnprocessableContent() }
                jsonPath("$.code") { value("serve_type_mismatch") }
            }
    }

    @Test
    fun `requires if-match for draft changes`() {
        val created = mockMvc.createDraft()
        val draftId = requireJsonString(created.response.contentAsString, "draftId")

        mockMvc
            .post("/rule-set-drafts/$draftId/flags") {
                contentType = MediaType.APPLICATION_JSON
                accept = MediaType.APPLICATION_JSON
                content = FLAG_JSON
            }.andExpect {
                status { isPreconditionRequired() }
                jsonPath("$.code") { value("PRECONDITION_REQUIRED") }
            }
    }

    @Test
    fun `rejects stale if-match for draft changes`() {
        val created = mockMvc.createDraft()
        val draftId = requireJsonString(created.response.contentAsString, "draftId")

        mockMvc
            .post("/rule-set-drafts/$draftId/flags") {
                contentType = MediaType.APPLICATION_JSON
                accept = MediaType.APPLICATION_JSON
                header(HttpHeaders.IF_MATCH, "\"99\"")
                content = FLAG_JSON
            }.andExpect {
                status { isPreconditionFailed() }
                jsonPath("$.code") { value("PRECONDITION_FAILED") }
                jsonPath("$.currentETag") { value("\"1\"") }
            }
    }
}
