package me.sensibile.augur.rule.api

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import kotlin.test.Test

@SpringBootTest
@AutoConfigureMockMvc
class RuleSetValidationHttpTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `validates canonical rule set json over http`() {
        mockMvc
            .post("/rule-sets/validate") {
                contentType = MediaType.APPLICATION_JSON
                accept = MediaType.APPLICATION_JSON
                content = validRuleSetJson()
            }.andExpect {
                status { isOk() }
                jsonPath("$.valid") { value(true) }
                jsonPath("$.summary.version") { value(1) }
                jsonPath("$.summary.flagCount") { value(1) }
                jsonPath("$.error") { doesNotExist() }
            }
    }

    @Test
    fun `returns unprocessable entity for invalid canonical rule set json over http`() {
        mockMvc
            .post("/rule-sets/validate") {
                contentType = MediaType.APPLICATION_JSON
                accept = MediaType.APPLICATION_JSON
                content = invalidServeTypeRuleSetJson()
            }.andExpect {
                status { isUnprocessableContent() }
                jsonPath("$.valid") { value(false) }
                jsonPath("$.summary") { doesNotExist() }
                jsonPath("$.error.code") { value("invalid_rule_set") }
                jsonPath("$.error.violations[0].code") { value("serve_type_mismatch") }
                jsonPath("$.error.violations[0].message") {
                    value(
                        "Rule 018ff7c1-9354-7b02-b021-76d2791d6a21 for flag new_checkout serves String but default value is Boolean.",
                    )
                }
            }
    }
}
