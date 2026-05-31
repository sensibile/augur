package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.flatMap
import me.sensibile.augur.rule.type

internal fun addRule(
    state: RuleSetDraftState?,
    command: AddRule,
    eventId: RuleManagementEventId,
): Outcome<RuleManagementCommandError, RuleAdded> =
    state
        .requireEditableFlag(command.draftId, command.flagKey)
        .flatMap { draft ->
            val existingRuleFlagKey = draft.findRuleFlagKey(command.rule.id)
            val flag = draft.flags.getValue(command.flagKey)
            val expectedServeType = flag.defaultValue.type()
            val actualServeType = command.rule.serve.type()
            when {
                existingRuleFlagKey != null -> {
                    Outcome.Err(
                        RuleManagementCommandError.RuleAlreadyExists(
                            draftId = command.draftId,
                            ruleId = command.rule.id,
                            existingFlagKey = existingRuleFlagKey,
                        ),
                    )
                }

                actualServeType != expectedServeType -> {
                    Outcome.Err(
                        RuleManagementCommandError.ServeTypeMismatch(
                            draftId = command.draftId,
                            flagKey = command.flagKey,
                            ruleId = command.rule.id,
                            expected = expectedServeType,
                            actual = actualServeType,
                        ),
                    )
                }

                else -> {
                    Outcome.Ok(
                        RuleAdded(
                            eventId = eventId,
                            draftId = command.draftId,
                            flagKey = command.flagKey,
                            rule = command.rule,
                        ),
                    )
                }
            }
        }

internal fun changeRuleCondition(
    state: RuleSetDraftState?,
    command: ChangeRuleCondition,
    eventId: RuleManagementEventId,
): Outcome<RuleManagementCommandError, RuleConditionChanged> =
    state
        .requireEditableRule(command.draftId, command.flagKey, command.ruleId)
        .flatMap {
            Outcome.Ok(
                RuleConditionChanged(
                    eventId = eventId,
                    draftId = command.draftId,
                    flagKey = command.flagKey,
                    ruleId = command.ruleId,
                    condition = command.condition,
                ),
            )
        }

internal fun changeRuleServeValue(
    state: RuleSetDraftState?,
    command: ChangeRuleServeValue,
    eventId: RuleManagementEventId,
): Outcome<RuleManagementCommandError, RuleServeValueChanged> =
    state
        .requireEditableRule(command.draftId, command.flagKey, command.ruleId)
        .flatMap { draft ->
            val flag = draft.flags.getValue(command.flagKey)
            val expectedType = flag.defaultValue.type()
            val actualType = command.serve.type()
            if (expectedType == actualType) {
                Outcome.Ok(
                    RuleServeValueChanged(
                        eventId = eventId,
                        draftId = command.draftId,
                        flagKey = command.flagKey,
                        ruleId = command.ruleId,
                        serve = command.serve,
                    ),
                )
            } else {
                Outcome.Err(
                    RuleManagementCommandError.ServeTypeMismatch(
                        draftId = command.draftId,
                        flagKey = command.flagKey,
                        ruleId = command.ruleId,
                        expected = expectedType,
                        actual = actualType,
                    ),
                )
            }
        }

internal fun removeRule(
    state: RuleSetDraftState?,
    command: RemoveRule,
    eventId: RuleManagementEventId,
): Outcome<RuleManagementCommandError, RuleRemoved> =
    state
        .requireEditableRule(command.draftId, command.flagKey, command.ruleId)
        .flatMap {
            Outcome.Ok(
                RuleRemoved(
                    eventId = eventId,
                    draftId = command.draftId,
                    flagKey = command.flagKey,
                    ruleId = command.ruleId,
                ),
            )
        }
