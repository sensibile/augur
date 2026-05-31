package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.Outcome

object RuleManagementCommandHandler {
    fun handle(
        state: RuleSetDraftState?,
        command: RuleManagementCommand,
        eventId: RuleManagementEventId,
    ): Outcome<RuleManagementCommandError, RuleManagementEvent> =
        when (command) {
            is CreateRuleSetDraft -> {
                createRuleSetDraft(state, command, eventId)
            }

            is AddFlag -> {
                addFlag(state, command, eventId)
            }

            is AddRule -> {
                addRule(state, command, eventId)
            }

            is EnableFlag -> {
                enableFlag(state, command, eventId)
            }

            is DisableFlag -> {
                disableFlag(state, command, eventId)
            }

            is ValidateRuleSetDraft -> {
                validateRuleSetDraft(state, command, eventId)
            }

            is ChangeRuleCondition -> {
                changeRuleCondition(state, command, eventId)
            }

            is ChangeRuleServeValue -> {
                changeRuleServeValue(state, command, eventId)
            }

            is RemoveRule -> {
                removeRule(state, command, eventId)
            }

            is PublishRuleSet -> {
                publishRuleSet(state, command, eventId)
            }

            is ArchiveRuleSet -> {
                archiveRuleSet(state, command, eventId)
            }
        }
}
