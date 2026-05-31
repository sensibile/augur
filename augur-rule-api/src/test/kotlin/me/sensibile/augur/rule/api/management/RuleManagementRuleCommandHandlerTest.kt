package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.Operator
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.RuleValue
import me.sensibile.augur.rule.type
import kotlin.test.Test
import kotlin.test.assertEquals

class RuleManagementRuleCommandHandlerTest {
    @Test
    fun `adds rule to editable draft flag`() {
        val flag = flag("new_checkout")
        val state = draftState(flags = mapOf(flag.key to flag))
        val eventId = eventId()
        val rule = rule()
        val command = AddRule(draftId = state.draftId, flagKey = flag.key, rule = rule)

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId)

        assertEquals(
            Outcome.Ok(RuleAdded(eventId = eventId, draftId = state.draftId, flagKey = flag.key, rule = rule)),
            actual,
        )
    }

    @Test
    fun `rejects add rule when flag does not exist`() {
        val state = draftState()
        val flagKey = flagKey("new_checkout")
        val command = AddRule(draftId = state.draftId, flagKey = flagKey, rule = rule())

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId())

        assertEquals(
            Outcome.Err(RuleManagementCommandError.FlagNotFound(state.draftId, flagKey)),
            actual,
        )
    }

    @Test
    fun `rejects add rule when rule id already exists in draft`() {
        val rule = rule()
        val existingFlag = flag("new_checkout", rules = listOf(rule))
        val targetFlag = flag("pricing_banner")
        val state =
            draftState(
                flags =
                    mapOf(
                        existingFlag.key to existingFlag,
                        targetFlag.key to targetFlag,
                    ),
            )
        val command = AddRule(draftId = state.draftId, flagKey = targetFlag.key, rule = rule)

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId())

        assertEquals(
            Outcome.Err(RuleManagementCommandError.RuleAlreadyExists(state.draftId, rule.id, existingFlag.key)),
            actual,
        )
    }

    @Test
    fun `rejects add rule when draft is not editable`() {
        val flag = flag("new_checkout")
        val state =
            draftState(
                flags = mapOf(flag.key to flag),
                status = RuleSetDraftStatus.Validated,
            )
        val command = AddRule(draftId = state.draftId, flagKey = flag.key, rule = rule())

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId())

        assertEquals(
            Outcome.Err(RuleManagementCommandError.DraftIsNotEditable(state.draftId, RuleSetDraftStatus.Validated)),
            actual,
        )
    }

    @Test
    fun `rejects add rule when serve type differs from flag default value`() {
        val flag = flag("new_checkout", defaultValue = RuleValue.boolean(false))
        val state = draftState(flags = mapOf(flag.key to flag))
        val rule = rule(serve = RuleValue.string("enabled"))
        val command = AddRule(draftId = state.draftId, flagKey = flag.key, rule = rule)

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId())

        assertEquals(
            Outcome.Err(
                RuleManagementCommandError.ServeTypeMismatch(
                    draftId = state.draftId,
                    flagKey = flag.key,
                    ruleId = rule.id,
                    expected = flag.defaultValue.type(),
                    actual = rule.serve.type(),
                ),
            ),
            actual,
        )
    }

    @Test
    fun `changes rule condition in editable draft`() {
        val rule = rule()
        val flag = flag("new_checkout", rules = listOf(rule))
        val state = draftState(flags = mapOf(flag.key to flag))
        val eventId = eventId()
        val condition = condition("tier", Operator.Eq, RuleValue.string("pro"))
        val command =
            ChangeRuleCondition(
                draftId = state.draftId,
                flagKey = flag.key,
                ruleId = rule.id,
                condition = condition,
            )

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId)

        assertEquals(
            Outcome.Ok(
                RuleConditionChanged(
                    eventId = eventId,
                    draftId = state.draftId,
                    flagKey = flag.key,
                    ruleId = rule.id,
                    condition = condition,
                ),
            ),
            actual,
        )
    }

    @Test
    fun `changes rule serve value in editable draft`() {
        val rule = rule(serve = RuleValue.boolean(false))
        val flag = flag("new_checkout", defaultValue = RuleValue.boolean(false), rules = listOf(rule))
        val state = draftState(flags = mapOf(flag.key to flag))
        val eventId = eventId()
        val serve = RuleValue.boolean(true)
        val command =
            ChangeRuleServeValue(
                draftId = state.draftId,
                flagKey = flag.key,
                ruleId = rule.id,
                serve = serve,
            )

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId)

        assertEquals(
            Outcome.Ok(
                RuleServeValueChanged(
                    eventId = eventId,
                    draftId = state.draftId,
                    flagKey = flag.key,
                    ruleId = rule.id,
                    serve = serve,
                ),
            ),
            actual,
        )
    }

    @Test
    fun `rejects rule change when rule does not exist`() {
        val flag = flag("new_checkout")
        val state = draftState(flags = mapOf(flag.key to flag))
        val missingRuleId = ruleId("018ff7c1-9354-7b02-b021-76d2791d6a24")
        val command =
            ChangeRuleCondition(
                draftId = state.draftId,
                flagKey = flag.key,
                ruleId = missingRuleId,
                condition = condition("tier", Operator.Eq, RuleValue.string("pro")),
            )

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId())

        assertEquals(
            Outcome.Err(RuleManagementCommandError.RuleNotFound(state.draftId, flag.key, missingRuleId)),
            actual,
        )
    }

    @Test
    fun `rejects rule change when flag does not exist`() {
        val state = draftState()
        val flagKey = flagKey("new_checkout")
        val ruleId = ruleId()
        val command =
            ChangeRuleServeValue(
                draftId = state.draftId,
                flagKey = flagKey,
                ruleId = ruleId,
                serve = RuleValue.boolean(true),
            )

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId())

        assertEquals(
            Outcome.Err(RuleManagementCommandError.FlagNotFound(state.draftId, flagKey)),
            actual,
        )
    }

    @Test
    fun `rejects rule change when draft is not editable`() {
        val rule = rule()
        val flag = flag("new_checkout", rules = listOf(rule))
        val state =
            draftState(
                flags = mapOf(flag.key to flag),
                status = RuleSetDraftStatus.Validated,
            )
        val command =
            ChangeRuleCondition(
                draftId = state.draftId,
                flagKey = flag.key,
                ruleId = rule.id,
                condition = condition("tier", Operator.Eq, RuleValue.string("pro")),
            )

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId())

        assertEquals(
            Outcome.Err(RuleManagementCommandError.DraftIsNotEditable(state.draftId, RuleSetDraftStatus.Validated)),
            actual,
        )
    }

    @Test
    fun `rejects serve value change when serve type differs from flag default value`() {
        val rule = rule(serve = RuleValue.boolean(false))
        val flag = flag("new_checkout", defaultValue = RuleValue.boolean(false), rules = listOf(rule))
        val state = draftState(flags = mapOf(flag.key to flag))
        val serve = RuleValue.string("enabled")
        val command =
            ChangeRuleServeValue(
                draftId = state.draftId,
                flagKey = flag.key,
                ruleId = rule.id,
                serve = serve,
            )

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId())

        assertEquals(
            Outcome.Err(
                RuleManagementCommandError.ServeTypeMismatch(
                    draftId = state.draftId,
                    flagKey = flag.key,
                    ruleId = rule.id,
                    expected = RuleValue.boolean(false).type(),
                    actual = serve.type(),
                ),
            ),
            actual,
        )
    }

    @Test
    fun `removes rule from editable draft`() {
        val rule = rule()
        val flag = flag("new_checkout", rules = listOf(rule))
        val state = draftState(flags = mapOf(flag.key to flag))
        val eventId = eventId()
        val command = RemoveRule(draftId = state.draftId, flagKey = flag.key, ruleId = rule.id)

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId)

        assertEquals(
            Outcome.Ok(
                RuleRemoved(
                    eventId = eventId,
                    draftId = state.draftId,
                    flagKey = flag.key,
                    ruleId = rule.id,
                ),
            ),
            actual,
        )
    }

    @Test
    fun `rejects remove rule when flag does not exist`() {
        val state = draftState()
        val flagKey = flagKey("new_checkout")
        val ruleId = ruleId()
        val command = RemoveRule(draftId = state.draftId, flagKey = flagKey, ruleId = ruleId)

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId())

        assertEquals(
            Outcome.Err(RuleManagementCommandError.FlagNotFound(state.draftId, flagKey)),
            actual,
        )
    }

    @Test
    fun `rejects remove rule when rule does not exist`() {
        val flag = flag("new_checkout")
        val state = draftState(flags = mapOf(flag.key to flag))
        val missingRuleId = ruleId("018ff7c1-9354-7b02-b021-76d2791d6a24")
        val command = RemoveRule(draftId = state.draftId, flagKey = flag.key, ruleId = missingRuleId)

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId())

        assertEquals(
            Outcome.Err(RuleManagementCommandError.RuleNotFound(state.draftId, flag.key, missingRuleId)),
            actual,
        )
    }

    @Test
    fun `rejects remove rule when draft is not editable`() {
        val rule = rule()
        val flag = flag("new_checkout", rules = listOf(rule))
        val state =
            draftState(
                flags = mapOf(flag.key to flag),
                status = RuleSetDraftStatus.Validated,
            )
        val command = RemoveRule(draftId = state.draftId, flagKey = flag.key, ruleId = rule.id)

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId())

        assertEquals(
            Outcome.Err(RuleManagementCommandError.DraftIsNotEditable(state.draftId, RuleSetDraftStatus.Validated)),
            actual,
        )
    }
}
