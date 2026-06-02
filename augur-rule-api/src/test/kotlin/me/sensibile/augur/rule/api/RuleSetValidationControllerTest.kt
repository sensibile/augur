package me.sensibile.augur.rule.api

import org.springframework.http.HttpStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RuleSetValidationControllerTest {
    @Test
    fun `returns ok for valid validation result`() {
        val controller = RuleSetValidationController(AlwaysValidRuleSetValidationService())

        val response = controller.validate("{}")

        assertEquals(HttpStatus.OK, response.statusCode)
        assertTrue(response.body!!.valid)
        assertEquals(RuleSetSnapshotSummary(version = 1, flagCount = 2), response.body!!.summary)
    }

    @Test
    fun `returns unprocessable entity for invalid validation result`() {
        val controller = RuleSetValidationController(AlwaysInvalidRuleSetValidationService())

        val exception =
            assertFailsWith<RuleSetValidationException> {
                controller.validate("{}")
            }

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.statusCode)
        assertEquals("invalid_json", exception.code)
        assertEquals("invalid", exception.body.detail)
    }
}

private class AlwaysValidRuleSetValidationService : RuleSetValidatorPort {
    override fun validateCanonicalJson(value: String): RuleSetValidationResult =
        RuleSetValidationResult.Valid(RuleSetSnapshotSummary(version = 1, flagCount = 2))
}

private class AlwaysInvalidRuleSetValidationService : RuleSetValidatorPort {
    override fun validateCanonicalJson(value: String): RuleSetValidationResult =
        RuleSetValidationResult.Invalid(
            RuleSetValidationErrorResponse(
                code = "invalid_json",
                message = "invalid",
            ),
        )
}
