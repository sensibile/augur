package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.FlagKey
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.RuleId
import me.sensibile.augur.rule.RuleSetValidationError
import me.sensibile.augur.rule.RuleSetValidator
import me.sensibile.augur.rule.flatMap

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

            is ChangeRuleCondition,
            is ChangeRuleServeValue,
            is RemoveRule,
            is PublishRuleSet,
            is ArchiveRuleSet,
            -> {
                Outcome.Err(RuleManagementCommandError.UnsupportedCommand(command::class.simpleName.orEmpty()))
            }
        }

    private fun createRuleSetDraft(
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

    private fun addFlag(
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

    private fun enableFlag(
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

    private fun disableFlag(
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

    private fun addRule(
        state: RuleSetDraftState?,
        command: AddRule,
        eventId: RuleManagementEventId,
    ): Outcome<RuleManagementCommandError, RuleAdded> =
        state
            .requireEditableFlag(command.draftId, command.flagKey)
            .flatMap { draft ->
                val existingRuleFlagKey = draft.findRuleFlagKey(command.rule.id)
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

    private fun validateRuleSetDraft(
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
}

sealed interface RuleManagementCommandError {
    data class DraftAlreadyExists(
        val draftId: RuleSetDraftId,
    ) : RuleManagementCommandError

    data class DraftNotFound(
        val draftId: RuleSetDraftId,
    ) : RuleManagementCommandError

    data class DraftIdMismatch(
        val expected: RuleSetDraftId,
        val actual: RuleSetDraftId,
    ) : RuleManagementCommandError

    data class DraftIsNotEditable(
        val draftId: RuleSetDraftId,
        val status: RuleSetDraftStatus,
    ) : RuleManagementCommandError

    data class FlagAlreadyExists(
        val draftId: RuleSetDraftId,
        val flagKey: FlagKey,
    ) : RuleManagementCommandError

    data class FlagNotFound(
        val draftId: RuleSetDraftId,
        val flagKey: FlagKey,
    ) : RuleManagementCommandError

    data class RuleAlreadyExists(
        val draftId: RuleSetDraftId,
        val ruleId: RuleId,
        val existingFlagKey: FlagKey,
    ) : RuleManagementCommandError

    data class InvalidRuleSetDraft(
        val draftId: RuleSetDraftId,
        val error: RuleSetValidationError,
    ) : RuleManagementCommandError

    data class UnsupportedCommand(
        val commandName: String,
    ) : RuleManagementCommandError
}

private fun RuleSetDraftState?.requireDraft(draftId: RuleSetDraftId): Outcome<RuleManagementCommandError, RuleSetDraftState> =
    when {
        this == null -> Outcome.Err(RuleManagementCommandError.DraftNotFound(draftId))
        this.draftId != draftId -> Outcome.Err(RuleManagementCommandError.DraftIdMismatch(expected = this.draftId, actual = draftId))
        else -> Outcome.Ok(this)
    }

private fun RuleSetDraftState?.requireEditableFlag(
    draftId: RuleSetDraftId,
    flagKey: FlagKey,
): Outcome<RuleManagementCommandError, RuleSetDraftState> =
    requireDraft(draftId)
        .flatMap { draft ->
            when {
                draft.status != RuleSetDraftStatus.Draft -> {
                    Outcome.Err(RuleManagementCommandError.DraftIsNotEditable(draftId, draft.status))
                }

                flagKey !in draft.flags -> {
                    Outcome.Err(RuleManagementCommandError.FlagNotFound(draftId, flagKey))
                }

                else -> {
                    Outcome.Ok(draft)
                }
            }
        }

private fun RuleSetDraftState.findRuleFlagKey(ruleId: RuleId): FlagKey? =
    flags.values
        .firstOrNull { flag ->
            flag.rules.any { rule -> rule.id == ruleId }
        }?.key
