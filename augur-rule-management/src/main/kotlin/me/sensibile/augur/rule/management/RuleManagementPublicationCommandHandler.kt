package me.sensibile.augur.rule.management

import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.flatMap

internal fun publishRuleSet(
    state: RuleSetDraftState?,
    command: PublishRuleSet,
    eventId: RuleManagementEventId,
): Outcome<RuleManagementCommandError, RuleSetPublished> =
    state
        .requireDraft(command.draftId)
        .flatMap { draft ->
            when {
                draft.status != RuleSetDraftStatus.Validated -> {
                    Outcome.Err(RuleManagementCommandError.DraftIsNotPublishable(command.draftId, draft.status))
                }

                else -> {
                    Outcome.Ok(
                        RuleSetPublished(
                            eventId = eventId,
                            draftId = command.draftId,
                            publishedRuleSetId = command.publishedRuleSetId,
                            ruleSetVersion = draft.ruleSetVersion,
                        ),
                    )
                }
            }
        }

internal fun archiveRuleSet(
    state: RuleSetDraftState?,
    command: ArchiveRuleSet,
    eventId: RuleManagementEventId,
): Outcome<RuleManagementCommandError, RuleSetArchived> =
    state
        .requireDraft(command.draftId)
        .flatMap { draft ->
            when {
                draft.status != RuleSetDraftStatus.Published -> {
                    Outcome.Err(RuleManagementCommandError.DraftIsNotArchivable(command.draftId, draft.status))
                }

                else -> {
                    Outcome.Ok(
                        RuleSetArchived(
                            eventId = eventId,
                            draftId = command.draftId,
                        ),
                    )
                }
            }
        }
