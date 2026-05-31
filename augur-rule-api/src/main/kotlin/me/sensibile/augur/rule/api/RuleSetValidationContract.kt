package me.sensibile.augur.rule.api

interface RuleSetValidatorPort {
    fun validateCanonicalJson(value: String): RuleSetValidationResult
}

sealed interface RuleSetValidationResult {
    data class Valid(
        val summary: RuleSetSnapshotSummary,
    ) : RuleSetValidationResult

    data class Invalid(
        val error: RuleSetValidationErrorResponse,
    ) : RuleSetValidationResult
}

data class RuleSetValidationResponse(
    val valid: Boolean,
    val summary: RuleSetSnapshotSummary? = null,
    val error: RuleSetValidationErrorResponse? = null,
)

data class RuleSetSnapshotSummary(
    val version: Long,
    val flagCount: Int,
)

data class RuleSetValidationErrorResponse(
    val code: String,
    val message: String,
    val violations: List<RuleSetViolationResponse> = emptyList(),
)

data class RuleSetViolationResponse(
    val code: String,
    val message: String,
)
