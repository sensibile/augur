package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.Condition
import me.sensibile.augur.rule.Flag
import me.sensibile.augur.rule.FlagKey
import me.sensibile.augur.rule.Rule
import me.sensibile.augur.rule.RuleId
import me.sensibile.augur.rule.RuleSetVersion
import me.sensibile.augur.rule.RuleValue

sealed interface RuleManagementCommand {
    val draftId: RuleSetDraftId
}

data class CreateRuleSetDraft(
    override val draftId: RuleSetDraftId,
    val ruleSetVersion: RuleSetVersion,
) : RuleManagementCommand

data class AddFlag(
    override val draftId: RuleSetDraftId,
    val flag: Flag,
) : RuleManagementCommand

data class EnableFlag(
    override val draftId: RuleSetDraftId,
    val flagKey: FlagKey,
) : RuleManagementCommand

data class DisableFlag(
    override val draftId: RuleSetDraftId,
    val flagKey: FlagKey,
) : RuleManagementCommand

data class AddRule(
    override val draftId: RuleSetDraftId,
    val flagKey: FlagKey,
    val rule: Rule,
) : RuleManagementCommand

data class ChangeRuleCondition(
    override val draftId: RuleSetDraftId,
    val flagKey: FlagKey,
    val ruleId: RuleId,
    val condition: Condition,
) : RuleManagementCommand

data class ChangeRuleServeValue(
    override val draftId: RuleSetDraftId,
    val flagKey: FlagKey,
    val ruleId: RuleId,
    val serve: RuleValue,
) : RuleManagementCommand

data class RemoveRule(
    override val draftId: RuleSetDraftId,
    val flagKey: FlagKey,
    val ruleId: RuleId,
) : RuleManagementCommand

data class ValidateRuleSetDraft(
    override val draftId: RuleSetDraftId,
) : RuleManagementCommand

data class PublishRuleSet(
    override val draftId: RuleSetDraftId,
    val publishedRuleSetId: PublishedRuleSetId,
) : RuleManagementCommand

data class ArchiveRuleSet(
    override val draftId: RuleSetDraftId,
) : RuleManagementCommand
