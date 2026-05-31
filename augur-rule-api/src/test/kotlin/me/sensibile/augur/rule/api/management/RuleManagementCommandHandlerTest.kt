package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.AttributeKey
import me.sensibile.augur.rule.Condition
import me.sensibile.augur.rule.Flag
import me.sensibile.augur.rule.FlagKey
import me.sensibile.augur.rule.Operator
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.Rule
import me.sensibile.augur.rule.RuleId
import me.sensibile.augur.rule.RuleSetVersion
import me.sensibile.augur.rule.RuleValue
import me.sensibile.augur.rule.type
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class RuleManagementCommandHandlerTest {
    @Test
    fun `creates draft when no state exists`() {
        val draftId = draftId()
        val eventId = eventId("018ff7c1-9354-7b02-b021-76d2791d6a22")
        val command = CreateRuleSetDraft(draftId = draftId, ruleSetVersion = version(1))

        val actual = RuleManagementCommandHandler.handle(state = null, command = command, eventId = eventId)

        assertEquals(
            Outcome.Ok(
                RuleSetDraftCreated(
                    eventId = eventId,
                    draftId = draftId,
                    ruleSetVersion = version(1),
                ),
            ),
            actual,
        )
    }

    @Test
    fun `rejects create draft when state already exists`() {
        val state = draftState()
        val command = CreateRuleSetDraft(draftId = state.draftId, ruleSetVersion = version(1))

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId())

        assertEquals(Outcome.Err(RuleManagementCommandError.DraftAlreadyExists(state.draftId)), actual)
    }

    @Test
    fun `adds flag to editable draft`() {
        val state = draftState()
        val eventId = eventId()
        val flag = flag("new_checkout")
        val command = AddFlag(draftId = state.draftId, flag = flag)

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId)

        assertEquals(
            Outcome.Ok(FlagAdded(eventId = eventId, draftId = state.draftId, flag = flag)),
            actual,
        )
    }

    @Test
    fun `rejects duplicate flag`() {
        val flag = flag("new_checkout")
        val state = draftState(flags = mapOf(flag.key to flag))
        val command = AddFlag(draftId = state.draftId, flag = flag)

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId())

        assertEquals(
            Outcome.Err(RuleManagementCommandError.FlagAlreadyExists(state.draftId, flag.key)),
            actual,
        )
    }

    @Test
    fun `enables flag in editable draft`() {
        val flag = flag("new_checkout", enabled = false)
        val state = draftState(flags = mapOf(flag.key to flag))
        val eventId = eventId()
        val command = EnableFlag(draftId = state.draftId, flagKey = flag.key)

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId)

        assertEquals(
            Outcome.Ok(FlagEnabled(eventId = eventId, draftId = state.draftId, flagKey = flag.key)),
            actual,
        )
    }

    @Test
    fun `disables flag in editable draft`() {
        val flag = flag("new_checkout", enabled = true)
        val state = draftState(flags = mapOf(flag.key to flag))
        val eventId = eventId()
        val command = DisableFlag(draftId = state.draftId, flagKey = flag.key)

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId)

        assertEquals(
            Outcome.Ok(FlagDisabled(eventId = eventId, draftId = state.draftId, flagKey = flag.key)),
            actual,
        )
    }

    @Test
    fun `rejects flag status change when flag does not exist`() {
        val state = draftState()
        val flagKey = flagKey("new_checkout")
        val command = EnableFlag(draftId = state.draftId, flagKey = flagKey)

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId())

        assertEquals(
            Outcome.Err(RuleManagementCommandError.FlagNotFound(state.draftId, flagKey)),
            actual,
        )
    }

    @Test
    fun `rejects flag status change when draft is not editable`() {
        val flag = flag("new_checkout")
        val state =
            draftState(
                flags = mapOf(flag.key to flag),
                status = RuleSetDraftStatus.Validated,
            )
        val command = DisableFlag(draftId = state.draftId, flagKey = flag.key)

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId())

        assertEquals(
            Outcome.Err(RuleManagementCommandError.DraftIsNotEditable(state.draftId, RuleSetDraftStatus.Validated)),
            actual,
        )
    }

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
    fun `validates valid draft`() {
        val state = draftState(flags = mapOf(flagKey("new_checkout") to flag("new_checkout")))
        val eventId = eventId()
        val command = ValidateRuleSetDraft(draftId = state.draftId)

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId)

        assertEquals(
            Outcome.Ok(
                RuleSetDraftValidated(
                    eventId = eventId,
                    draftId = state.draftId,
                    ruleSetVersion = state.ruleSetVersion,
                ),
            ),
            actual,
        )
    }

    @Test
    fun `rejects invalid draft`() {
        val state =
            draftState(
                flags =
                    mapOf(
                        flagKey("new_checkout") to
                            flag(
                                key = "wrong_key",
                            ),
                    ),
            )
        val command = ValidateRuleSetDraft(draftId = state.draftId)

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId())

        assertIs<Outcome.Err<RuleManagementCommandError.InvalidRuleSetDraft>>(actual)
    }

    @Test
    fun `rejects unsupported commands explicitly`() {
        val state = draftState()
        val command = ArchiveRuleSet(draftId = state.draftId)

        val actual = RuleManagementCommandHandler.handle(state = state, command = command, eventId = eventId())

        assertEquals(Outcome.Err(RuleManagementCommandError.UnsupportedCommand("ArchiveRuleSet")), actual)
    }

    private fun draftState(
        flags: Map<FlagKey, Flag> = emptyMap(),
        status: RuleSetDraftStatus = RuleSetDraftStatus.Draft,
    ): RuleSetDraftState =
        RuleSetDraftState(
            draftId = draftId(),
            ruleSetVersion = version(1),
            flags = flags,
            status = status,
        )

    private fun flag(
        key: String,
        enabled: Boolean = true,
        defaultValue: RuleValue = RuleValue.boolean(false),
        rules: List<Rule> = emptyList(),
    ): Flag =
        Flag(
            key = flagKey(key),
            enabled = enabled,
            defaultValue = defaultValue,
            rules = rules,
        )

    private fun rule(serve: RuleValue = RuleValue.boolean(true)): Rule =
        Rule(
            id = ruleId(),
            condition = condition("country", Operator.Eq, RuleValue.string("KR")),
            serve = serve,
        )

    private fun condition(
        attributeKey: String,
        operator: Operator,
        value: RuleValue,
    ): Condition =
        Condition.Predicate(
            attributeKey = attributeKey(attributeKey),
            operator = operator,
            value = value,
        )

    private fun draftId(): RuleSetDraftId =
        when (val draftId = RuleSetDraftId.of(Uuid.parse("018ff7c1-9354-7b02-b021-76d2791d6a21"))) {
            is Outcome.Err -> error("Invalid test draft id: ${draftId.error}")
            is Outcome.Ok -> draftId.value
        }

    private fun eventId(value: String = "018ff7c1-9354-7b02-b021-76d2791d6a22"): RuleManagementEventId =
        when (val eventId = RuleManagementEventId.of(Uuid.parse(value))) {
            is Outcome.Err -> error("Invalid test event id: ${eventId.error}")
            is Outcome.Ok -> eventId.value
        }

    private fun version(value: Long): RuleSetVersion =
        when (val version = RuleSetVersion.of(value)) {
            is Outcome.Err -> error("Invalid test version: ${version.error}")
            is Outcome.Ok -> version.value
        }

    private fun flagKey(value: String): FlagKey =
        when (val key = FlagKey.of(value)) {
            is Outcome.Err -> error("Invalid test flag key: ${key.error}")
            is Outcome.Ok -> key.value
        }

    private fun attributeKey(value: String): AttributeKey =
        when (val key = AttributeKey.of(value)) {
            is Outcome.Err -> error("Invalid test attribute key: ${key.error}")
            is Outcome.Ok -> key.value
        }

    private fun ruleId(value: String = "018ff7c1-9354-7b02-b021-76d2791d6a23"): RuleId =
        when (val ruleId = RuleId.of(Uuid.parse(value))) {
            is Outcome.Err -> error("Invalid test rule id: ${ruleId.error}")
            is Outcome.Ok -> ruleId.value
        }
}
