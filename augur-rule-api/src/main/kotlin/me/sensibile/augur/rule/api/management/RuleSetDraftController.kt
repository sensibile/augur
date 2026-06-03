@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.FlagKey
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.RuleSetVersion
import me.sensibile.augur.rule.api.toValidationError
import me.sensibile.augur.rule.json.RuleJson
import me.sensibile.augur.rule.management.AddFlag
import me.sensibile.augur.rule.management.AddRule
import me.sensibile.augur.rule.management.CreateRuleSetDraft
import me.sensibile.augur.rule.management.RuleManagementCommand
import me.sensibile.augur.rule.management.RuleManagementCommandService
import me.sensibile.augur.rule.management.RuleManagementCommandServiceError
import me.sensibile.augur.rule.management.RuleManagementEvent
import me.sensibile.augur.rule.management.RuleManagementEventIdGenerator
import me.sensibile.augur.rule.management.RuleManagementEventReplayer
import me.sensibile.augur.rule.management.RuleManagementEventStore
import me.sensibile.augur.rule.management.RuleManagementStreamVersion
import me.sensibile.augur.rule.management.RuleSetDraftId
import me.sensibile.augur.rule.management.RuleSetDraftIdGenerator
import me.sensibile.augur.rule.management.RuleSetDraftState
import me.sensibile.augur.rule.management.ValidateRuleSetDraft
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.uuid.Uuid

@RestController
@RequestMapping("/rule-set-drafts")
class RuleSetDraftController(
    private val draftIds: RuleSetDraftIdGenerator,
    private val eventIds: RuleManagementEventIdGenerator,
    private val commandService: RuleManagementCommandService,
    private val eventStore: RuleManagementEventStore,
) {
    @PostMapping(consumes = ["application/json"], produces = ["application/json"])
    fun create(
        @RequestBody request: CreateRuleSetDraftRequest,
    ): ResponseEntity<RuleSetDraftCommandResponse> {
        val draftId = draftIds.nextRuleSetDraftId()
        val version = request.version.toRuleSetVersion()
        val result = handle(CreateRuleSetDraft(draftId = draftId, ruleSetVersion = version))
        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }

    @GetMapping("/{draftId}", produces = ["application/json"])
    fun get(
        @PathVariable draftId: String,
    ): RuleSetDraftResponse {
        val parsedDraftId = draftId.toDraftId()
        val loaded = loadDraft(parsedDraftId)
        return loaded.state.toResponse(loaded.streamVersion)
    }

    @PostMapping("/{draftId}/flags", consumes = ["application/json"], produces = ["application/json"])
    fun addFlag(
        @PathVariable draftId: String,
        @RequestBody body: String,
    ): RuleSetDraftCommandResponse {
        val parsedDraftId = draftId.toDraftId()
        val flag =
            when (val decoded = RuleJson.decodeFlag(body)) {
                is Outcome.Err -> throw RuleJsonException(decoded.error.toValidationError())
                is Outcome.Ok -> decoded.value
            }
        return handle(AddFlag(draftId = parsedDraftId, flag = flag))
    }

    @PostMapping("/{draftId}/flags/{flagKey}/rules", consumes = ["application/json"], produces = ["application/json"])
    fun addRule(
        @PathVariable draftId: String,
        @PathVariable flagKey: String,
        @RequestBody body: String,
    ): RuleSetDraftCommandResponse {
        val parsedDraftId = draftId.toDraftId()
        val parsedFlagKey = flagKey.toFlagKey()
        val rule =
            when (val decoded = RuleJson.decodeRule(body)) {
                is Outcome.Err -> throw RuleJsonException(decoded.error.toValidationError())
                is Outcome.Ok -> decoded.value
            }
        return handle(AddRule(draftId = parsedDraftId, flagKey = parsedFlagKey, rule = rule))
    }

    @PostMapping("/{draftId}/validate", produces = ["application/json"])
    fun validate(
        @PathVariable draftId: String,
    ): RuleSetDraftCommandResponse = handle(ValidateRuleSetDraft(draftId = draftId.toDraftId()))

    private fun handle(command: RuleManagementCommand): RuleSetDraftCommandResponse =
        when (val handled = commandService.handle(command = command, eventId = eventIds.nextRuleManagementEventId())) {
            is Outcome.Err -> {
                throw handled.error.toApiException()
            }

            is Outcome.Ok -> {
                RuleSetDraftCommandResponse(
                    eventType = handled.value.event.eventType,
                    draft = handled.value.state.toResponse(handled.value.streamVersion),
                )
            }
        }

    private fun loadDraft(draftId: RuleSetDraftId): LoadedDraft {
        val stream = draftId.loadStream()
        val state = draftId.replayStream(stream.events)

        return LoadedDraft(state = state, streamVersion = stream.version)
    }

    private fun RuleSetDraftId.loadStream() =
        when (val loaded = eventStore.load(this)) {
            is Outcome.Err -> {
                throw loaded.error.toApiException()
            }

            is Outcome.Ok -> {
                loaded.value
            }
        }

    private fun RuleSetDraftId.replayStream(events: Sequence<RuleManagementEvent>): RuleSetDraftState =
        when (val replayed = RuleManagementEventReplayer.replay(events)) {
            is Outcome.Err -> {
                throw RuleManagementCommandServiceError.EventReplayFailed(replayed.error).toApiException()
            }

            is Outcome.Ok -> {
                replayed.value ?: throw DraftNotFoundException(this)
            }
        }
}

private data class LoadedDraft(
    val state: RuleSetDraftState,
    val streamVersion: RuleManagementStreamVersion?,
)

private fun RuleSetDraftState.toResponse(streamVersion: RuleManagementStreamVersion?): RuleSetDraftResponse =
    RuleSetDraftResponse(
        draftId = draftId.value.toString(),
        ruleSetVersion = ruleSetVersion.value,
        status = status.name,
        flagCount = flags.size,
        streamVersion = streamVersion?.value,
    )

private val RuleManagementEvent.eventType: String
    get() = this::class.simpleName.orEmpty()

private fun Long.toRuleSetVersion(): RuleSetVersion =
    when (val version = RuleSetVersion.of(this)) {
        is Outcome.Err -> throw ValueObjectException(version.error)
        is Outcome.Ok -> version.value
    }

private fun String.toDraftId(): RuleSetDraftId {
    val uuid =
        try {
            Uuid.parse(this)
        } catch (exception: IllegalArgumentException) {
            throw BadRequestException(
                code = "invalid_draft_id",
                detail = "Draft id must be a UUID.",
                cause = exception,
            )
        }
    return when (val draftId = RuleSetDraftId.of(uuid)) {
        is Outcome.Err -> throw ValueObjectException(draftId.error)
        is Outcome.Ok -> draftId.value
    }
}

private fun String.toFlagKey(): FlagKey =
    when (val key = FlagKey.of(this)) {
        is Outcome.Err -> throw ValueObjectException(key.error)
        is Outcome.Ok -> key.value
    }
