package me.sensibile.augur.rule.management

import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.RuleSetValidator
import me.sensibile.augur.rule.flatMap

internal fun createRuleSetDraft(
    state: RuleSetDraftState?,
    command: CreateRuleSetDraft,
    eventId: RuleManagementEventId,
): Outcome<RuleManagementCommandError, RuleSetDraftCreated> =
    if (state == null) {
        Outcome.Ok(
            RuleSetDraftCreated(
                eventId = eventId,
                draftId = command.draftId,
                ruleSetVersion = command.ruleSetVersion,
            ),
        )
    } else {
        Outcome.Err(RuleManagementCommandError.DraftAlreadyExists(command.draftId))
    }

internal fun validateRuleSetDraft(
    state: RuleSetDraftState?,
    command: ValidateRuleSetDraft,
    eventId: RuleManagementEventId,
): Outcome<RuleManagementCommandError, RuleSetDraftValidated> =
    state
        .requireDraft(command.draftId)
        .flatMap { draft ->
            when (val validated = RuleSetValidator.validate(draft.toRuleSet())) {
                is Outcome.Err -> {
                    Outcome.Err(RuleManagementCommandError.InvalidRuleSetDraft(command.draftId, validated.error))
                }

                is Outcome.Ok -> {
                    Outcome.Ok(
                        RuleSetDraftValidated(
                            eventId = eventId,
                            draftId = command.draftId,
                            ruleSetVersion = validated.value.value.version,
                        ),
                    )
                }
            }
        }
