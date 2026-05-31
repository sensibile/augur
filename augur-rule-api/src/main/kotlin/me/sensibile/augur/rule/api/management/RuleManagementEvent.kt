package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.Condition
import me.sensibile.augur.rule.Flag
import me.sensibile.augur.rule.FlagKey
import me.sensibile.augur.rule.Rule
import me.sensibile.augur.rule.RuleId
import me.sensibile.augur.rule.RuleSetVersion
import me.sensibile.augur.rule.RuleValue

sealed interface RuleManagementEvent {
    val eventId: RuleManagementEventId
    val draftId: RuleSetDraftId
}

data class RuleSetDraftCreated(
    override val eventId: RuleManagementEventId,
    override val draftId: RuleSetDraftId,
    val ruleSetVersion: RuleSetVersion,
) : RuleManagementEvent

data class FlagAdded(
    override val eventId: RuleManagementEventId,
    override val draftId: RuleSetDraftId,
    val flag: Flag,
) : RuleManagementEvent

data class FlagEnabled(
    override val eventId: RuleManagementEventId,
    override val draftId: RuleSetDraftId,
    val flagKey: FlagKey,
) : RuleManagementEvent

data class FlagDisabled(
    override val eventId: RuleManagementEventId,
    override val draftId: RuleSetDraftId,
    val flagKey: FlagKey,
) : RuleManagementEvent

data class RuleAdded(
    override val eventId: RuleManagementEventId,
    override val draftId: RuleSetDraftId,
    val flagKey: FlagKey,
    val rule: Rule,
) : RuleManagementEvent

data class RuleConditionChanged(
    override val eventId: RuleManagementEventId,
    override val draftId: RuleSetDraftId,
    val flagKey: FlagKey,
    val ruleId: RuleId,
    val condition: Condition,
) : RuleManagementEvent

data class RuleServeValueChanged(
    override val eventId: RuleManagementEventId,
    override val draftId: RuleSetDraftId,
    val flagKey: FlagKey,
    val ruleId: RuleId,
    val serve: RuleValue,
) : RuleManagementEvent

data class RuleRemoved(
    override val eventId: RuleManagementEventId,
    override val draftId: RuleSetDraftId,
    val flagKey: FlagKey,
    val ruleId: RuleId,
) : RuleManagementEvent

data class RuleSetDraftValidated(
    override val eventId: RuleManagementEventId,
    override val draftId: RuleSetDraftId,
    val ruleSetVersion: RuleSetVersion,
) : RuleManagementEvent

data class RuleSetPublished(
    override val eventId: RuleManagementEventId,
    override val draftId: RuleSetDraftId,
    val publishedRuleSetId: PublishedRuleSetId,
    val ruleSetVersion: RuleSetVersion,
) : RuleManagementEvent

data class RuleSetArchived(
    override val eventId: RuleManagementEventId,
    override val draftId: RuleSetDraftId,
) : RuleManagementEvent
