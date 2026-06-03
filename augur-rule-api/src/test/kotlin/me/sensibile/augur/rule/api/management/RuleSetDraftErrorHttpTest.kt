package me.sensibile.augur.rule.api.management

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import kotlin.test.Test

@SpringBootTest
@AutoConfigureMockMvc
class RuleSetDraftErrorHttpTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `returns conflict for duplicate flag`() {
        val created = mockMvc.createDraft()
        val draftId = requireJsonString(created.response.contentAsString, "draftId")
        val flagAdded = mockMvc.addFlag(draftId, requireHeader(created, HttpHeaders.ETAG))

        mockMvc
            .post("/rule-set-drafts/$draftId/flags") {
                contentType = MediaType.APPLICATION_JSON
                accept = MediaType.APPLICATION_JSON
                header(HttpHeaders.IF_MATCH, requireHeader(flagAdded, HttpHeaders.ETAG))
                content = FLAG_JSON
            }.andExpect {
                status { isConflict() }
                jsonPath("$.title") { value("Rule management command rejected") }
                jsonPath("$.code") { value("flag_already_exists") }
                jsonPath("$.detail") { value("Flag new_checkout already exists in draft $draftId.") }
            }
    }

    @Test
    fun `returns conflict when editing validated draft`() {
        val created = mockMvc.createDraft()
        val draftId = requireJsonString(created.response.contentAsString, "draftId")
        val flagAdded = mockMvc.addFlag(draftId, requireHeader(created, HttpHeaders.ETAG))
        val validated = mockMvc.validateDraft(draftId, requireHeader(flagAdded, HttpHeaders.ETAG))

        mockMvc
            .post("/rule-set-drafts/$draftId/flags") {
                contentType = MediaType.APPLICATION_JSON
                accept = MediaType.APPLICATION_JSON
                header(HttpHeaders.IF_MATCH, requireHeader(validated, HttpHeaders.ETAG))
                content = SECOND_FLAG_JSON
            }.andExpect {
                status { isConflict() }
                jsonPath("$.title") { value("Rule management command rejected") }
                jsonPath("$.code") { value("draft_is_not_editable") }
                jsonPath("$.detail") { value("Rule set draft $draftId is Validated and cannot be edited.") }
            }
    }

    @Test
    fun `returns unprocessable entity for invalid draft validation`() {
        val created = mockMvc.createDraft()
        val draftId = requireJsonString(created.response.contentAsString, "draftId")
        val flagAdded = mockMvc.addFlag(draftId, requireHeader(created, HttpHeaders.ETAG), INVALID_FLAG_JSON)

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
        val created = mockMvc.createDraft()
        val draftId = requireJsonString(created.response.contentAsString, "draftId")

        mockMvc
            .post("/rule-set-drafts/$draftId/flags/INVALID/rules") {
                contentType = MediaType.APPLICATION_JSON
                accept = MediaType.APPLICATION_JSON
                header(HttpHeaders.IF_MATCH, requireHeader(created, HttpHeaders.ETAG))
                content = RULE_JSON
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.title") { value("Rule management request is invalid") }
                jsonPath("$.code") { value("invalid_flag_key") }
                jsonPath("$.detail") { value("Invalid value for flagKey.") }
            }
    }
}
