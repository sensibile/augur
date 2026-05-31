package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.Flag
import me.sensibile.augur.rule.FlagKey
import me.sensibile.augur.rule.RuleSet
import me.sensibile.augur.rule.RuleSetVersion

data class RuleSetDraftState(
    val draftId: RuleSetDraftId,
    val ruleSetVersion: RuleSetVersion,
    val flags: Map<FlagKey, Flag>,
    val status: RuleSetDraftStatus,
) {
    fun toRuleSet(): RuleSet =
        RuleSet(
            version = ruleSetVersion,
            flags = flags,
        )
}

enum class RuleSetDraftStatus {
    Draft,
    Validated,
    Published,
    Archived,
}
