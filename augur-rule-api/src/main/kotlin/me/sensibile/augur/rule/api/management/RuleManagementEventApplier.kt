package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.Flag
import me.sensibile.augur.rule.FlagKey
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.Rule
import me.sensibile.augur.rule.RuleId
import me.sensibile.augur.rule.flatMap
import me.sensibile.augur.rule.map

object RuleManagementEventApplier {
    fun apply(
        state: RuleSetDraftState?,
        event: RuleManagementEvent,
    ): Outcome<RuleManagementEventApplyError, RuleSetDraftState> =
        when (event) {
            is RuleSetDraftCreated -> {
                applyDraftCreated(state, event)
            }

            is FlagAdded -> {
                applyFlagAdded(state, event)
            }

            is FlagEnabled -> {
                state.updateFlag(event, event.flagKey) { flag ->
                    flag.copy(enabled = true)
                }
            }

            is FlagDisabled -> {
                state.updateFlag(event, event.flagKey) { flag ->
                    flag.copy(enabled = false)
                }
            }

            is RuleAdded -> {
                state.updateFlag(event, event.flagKey) { flag ->
                    flag.copy(rules = flag.rules + event.rule)
                }
            }

            is RuleConditionChanged -> {
                state.updateRule(event, event.flagKey, event.ruleId) { rule ->
                    rule.copy(condition = event.condition)
                }
            }

            is RuleServeValueChanged -> {
                state.updateRule(event, event.flagKey, event.ruleId) { rule ->
                    rule.copy(serve = event.serve)
                }
            }

            is RuleRemoved -> {
                state.removeRule(event, event.flagKey, event.ruleId)
            }

            is RuleSetDraftValidated -> {
                state.updateStatus(event, RuleSetDraftStatus.Validated)
            }

            is RuleSetPublished -> {
                state.updateStatus(event, RuleSetDraftStatus.Published)
            }

            is RuleSetArchived -> {
                state.updateStatus(event, RuleSetDraftStatus.Archived)
            }
        }

    private fun applyDraftCreated(
        state: RuleSetDraftState?,
        event: RuleSetDraftCreated,
    ): Outcome<RuleManagementEventApplyError, RuleSetDraftState> =
        if (state == null) {
            Outcome.Ok(
                RuleSetDraftState(
                    draftId = event.draftId,
                    ruleSetVersion = event.ruleSetVersion,
                    flags = emptyMap(),
                    status = RuleSetDraftStatus.Draft,
                ),
            )
        } else {
            Outcome.Err(RuleManagementEventApplyError.DraftAlreadyExists(event.draftId))
        }

    private fun applyFlagAdded(
        state: RuleSetDraftState?,
        event: FlagAdded,
    ): Outcome<RuleManagementEventApplyError, RuleSetDraftState> =
        state
            .requireState(event)
            .map { draft ->
                draft.copy(
                    flags = draft.flags + (event.flag.key to event.flag),
                    status = RuleSetDraftStatus.Draft,
                )
            }

    private fun RuleSetDraftState?.updateStatus(
        event: RuleManagementEvent,
        status: RuleSetDraftStatus,
    ): Outcome<RuleManagementEventApplyError, RuleSetDraftState> =
        requireState(event).map { draft ->
            draft.copy(status = status)
        }

    private fun RuleSetDraftState?.updateRule(
        event: RuleManagementEvent,
        flagKey: FlagKey,
        ruleId: RuleId,
        transform: (Rule) -> Rule,
    ): Outcome<RuleManagementEventApplyError, RuleSetDraftState> =
        updateFlag(event, flagKey) { flag ->
            flag.copy(
                rules =
                    flag.rules.map { rule ->
                        if (rule.id == ruleId) {
                            transform(rule)
                        } else {
                            rule
                        }
                    },
            )
        }.flatMap { draft ->
            if (draft.containsRule(flagKey, ruleId)) {
                Outcome.Ok(draft)
            } else {
                Outcome.Err(RuleManagementEventApplyError.RuleNotFound(event.draftId, flagKey, ruleId))
            }
        }

    private fun RuleSetDraftState?.removeRule(
        event: RuleManagementEvent,
        flagKey: FlagKey,
        ruleId: RuleId,
    ): Outcome<RuleManagementEventApplyError, RuleSetDraftState> =
        requireState(event).flatMap { draft ->
            val flag = draft.flags[flagKey]
            when {
                flag == null -> {
                    Outcome.Err(RuleManagementEventApplyError.FlagNotFound(event.draftId, flagKey))
                }

                flag.rules.none { rule -> rule.id == ruleId } -> {
                    Outcome.Err(RuleManagementEventApplyError.RuleNotFound(event.draftId, flagKey, ruleId))
                }

                else -> {
                    val updatedFlag =
                        flag.copy(
                            rules = flag.rules.filterNot { rule -> rule.id == ruleId },
                        )
                    Outcome.Ok(
                        draft.copy(
                            flags = draft.flags + (flagKey to updatedFlag),
                            status = RuleSetDraftStatus.Draft,
                        ),
                    )
                }
            }
        }

    private fun RuleSetDraftState?.updateFlag(
        event: RuleManagementEvent,
        flagKey: FlagKey,
        transform: (Flag) -> Flag,
    ): Outcome<RuleManagementEventApplyError, RuleSetDraftState> =
        requireState(event).flatMap { draft ->
            val flag = draft.flags[flagKey]
            if (flag == null) {
                Outcome.Err(RuleManagementEventApplyError.FlagNotFound(event.draftId, flagKey))
            } else {
                Outcome.Ok(
                    draft.copy(
                        flags = draft.flags + (flagKey to transform(flag)),
                        status = RuleSetDraftStatus.Draft,
                    ),
                )
            }
        }

    private fun RuleSetDraftState?.requireState(event: RuleManagementEvent): Outcome<RuleManagementEventApplyError, RuleSetDraftState> =
        when {
            this == null -> {
                Outcome.Err(RuleManagementEventApplyError.DraftNotFound(event.draftId))
            }

            draftId != event.draftId -> {
                Outcome.Err(
                    RuleManagementEventApplyError.DraftIdMismatch(
                        expected = draftId,
                        actual = event.draftId,
                    ),
                )
            }

            else -> {
                Outcome.Ok(this)
            }
        }

    private fun RuleSetDraftState.containsRule(
        flagKey: FlagKey,
        ruleId: RuleId,
    ): Boolean = flags.getValue(flagKey).rules.any { rule -> rule.id == ruleId }
}

sealed interface RuleManagementEventApplyError {
    data class DraftAlreadyExists(
        val draftId: RuleSetDraftId,
    ) : RuleManagementEventApplyError

    data class DraftNotFound(
        val draftId: RuleSetDraftId,
    ) : RuleManagementEventApplyError

    data class DraftIdMismatch(
        val expected: RuleSetDraftId,
        val actual: RuleSetDraftId,
    ) : RuleManagementEventApplyError

    data class FlagNotFound(
        val draftId: RuleSetDraftId,
        val flagKey: FlagKey,
    ) : RuleManagementEventApplyError

    data class RuleNotFound(
        val draftId: RuleSetDraftId,
        val flagKey: FlagKey,
        val ruleId: RuleId,
    ) : RuleManagementEventApplyError
}
