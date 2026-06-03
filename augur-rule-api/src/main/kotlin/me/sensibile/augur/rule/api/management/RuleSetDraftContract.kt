package me.sensibile.augur.rule.api.management

data class CreateRuleSetDraftRequest(
    val version: Long,
)

data class RuleSetDraftResponse(
    val draftId: String,
    val ruleSetVersion: Long,
    val status: String,
    val flagCount: Int,
    val streamVersion: Long?,
)

data class RuleSetDraftCommandResponse(
    val eventType: String,
    val draft: RuleSetDraftResponse,
)
