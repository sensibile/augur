package me.sensibile.augur.rule

data class RuleSet(
    val version: RuleSetVersion,
    val flags: Map<FlagKey, Flag>,
)

data class Flag(
    val key: FlagKey,
    val enabled: Boolean,
    val defaultValue: RuleValue,
    val rules: List<Rule>,
)

data class Rule(
    val id: RuleId,
    val condition: Condition,
    val serve: RuleValue,
)
