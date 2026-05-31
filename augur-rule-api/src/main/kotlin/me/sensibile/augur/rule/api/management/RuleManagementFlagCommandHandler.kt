package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.flatMap

internal fun addFlag(
    state: RuleSetDraftState?,
    command: AddFlag,
    eventId: RuleManagementEventId,
): Outcome<RuleManagementCommandError, FlagAdded> =
    state
        .requireDraft(command.draftId)
        .flatMap { draft ->
            when {
                draft.status != RuleSetDraftStatus.Draft -> {
                    Outcome.Err(RuleManagementCommandError.DraftIsNotEditable(command.draftId, draft.status))
                }

                command.flag.key in draft.flags -> {
                    Outcome.Err(RuleManagementCommandError.FlagAlreadyExists(command.draftId, command.flag.key))
                }

                else -> {
                    Outcome.Ok(
                        FlagAdded(
                            eventId = eventId,
                            draftId = command.draftId,
                            flag = command.flag,
                        ),
                    )
                }
            }
        }

internal fun enableFlag(
    state: RuleSetDraftState?,
    command: EnableFlag,
    eventId: RuleManagementEventId,
): Outcome<RuleManagementCommandError, FlagEnabled> =
    state
        .requireEditableFlag(command.draftId, command.flagKey)
        .flatMap {
            Outcome.Ok(
                FlagEnabled(
                    eventId = eventId,
                    draftId = command.draftId,
                    flagKey = command.flagKey,
                ),
            )
        }

internal fun disableFlag(
    state: RuleSetDraftState?,
    command: DisableFlag,
    eventId: RuleManagementEventId,
): Outcome<RuleManagementCommandError, FlagDisabled> =
    state
        .requireEditableFlag(command.draftId, command.flagKey)
        .flatMap {
            Outcome.Ok(
                FlagDisabled(
                    eventId = eventId,
                    draftId = command.draftId,
                    flagKey = command.flagKey,
                ),
            )
        }
