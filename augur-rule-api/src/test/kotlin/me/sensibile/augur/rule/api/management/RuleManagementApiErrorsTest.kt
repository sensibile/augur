@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.FlagKey
import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.RuleId
import me.sensibile.augur.rule.ValueObjectError
import me.sensibile.augur.rule.management.RuleManagementCommandError
import me.sensibile.augur.rule.management.RuleManagementCommandServiceError
import me.sensibile.augur.rule.management.RuleManagementEventApplyError
import me.sensibile.augur.rule.management.RuleManagementEventStoreError
import me.sensibile.augur.rule.management.RuleManagementExpectedStreamVersion
import me.sensibile.augur.rule.management.RuleManagementProcessError
import me.sensibile.augur.rule.management.RuleSetDraftId
import me.sensibile.augur.rule.management.RuleSetDraftStatus
import org.springframework.http.HttpStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class RuleManagementApiErrorsTest {
    @Test
    fun `maps draft command errors not reached by current http surface`() {
        assertApiException(
            expectedStatus = HttpStatus.CONFLICT,
            expectedCode = "draft_already_exists",
            expectedDetail = "Rule set draft ${draftId()} already exists.",
            actual = RuleManagementCommandError.DraftAlreadyExists(draftId()).toApiException(),
        )
        assertApiException(
            expectedStatus = HttpStatus.NOT_FOUND,
            expectedCode = "draft_not_found",
            expectedDetail = "Rule set draft ${draftId()} was not found.",
            actual = RuleManagementCommandError.DraftNotFound(draftId()).toApiException(),
        )
        assertApiException(
            expectedStatus = HttpStatus.CONFLICT,
            expectedCode = "draft_id_mismatch",
            expectedDetail = "Expected draft ${draftId()} but command targeted ${draftId(SECOND_DRAFT_ID)}.",
            actual = RuleManagementCommandError.DraftIdMismatch(draftId(), draftId(SECOND_DRAFT_ID)).toApiException(),
        )
        assertApiException(
            expectedStatus = HttpStatus.CONFLICT,
            expectedCode = "draft_is_not_publishable",
            expectedDetail = "Rule set draft ${draftId()} is Draft and cannot be published.",
            actual = RuleManagementCommandError.DraftIsNotPublishable(draftId(), RuleSetDraftStatus.Draft).toApiException(),
        )
        assertApiException(
            expectedStatus = HttpStatus.CONFLICT,
            expectedCode = "draft_is_not_archivable",
            expectedDetail = "Rule set draft ${draftId()} is Published and cannot be archived.",
            actual = RuleManagementCommandError.DraftIsNotArchivable(draftId(), RuleSetDraftStatus.Published).toApiException(),
        )
    }

    @Test
    fun `maps rule command errors not reached by current http surface`() {
        assertApiException(
            expectedStatus = HttpStatus.CONFLICT,
            expectedCode = "rule_already_exists",
            expectedDetail = "Rule ${ruleId()} already exists under flag new_checkout in draft ${draftId()}.",
            actual =
                RuleManagementCommandError
                    .RuleAlreadyExists(
                        draftId = draftId(),
                        ruleId = ruleId(),
                        existingFlagKey = flagKey("new_checkout"),
                    ).toApiException(),
        )
        assertApiException(
            expectedStatus = HttpStatus.NOT_FOUND,
            expectedCode = "rule_not_found",
            expectedDetail = "Rule ${ruleId()} was not found under flag new_checkout in draft ${draftId()}.",
            actual =
                RuleManagementCommandError
                    .RuleNotFound(
                        draftId = draftId(),
                        flagKey = flagKey("new_checkout"),
                        ruleId = ruleId(),
                    ).toApiException(),
        )
    }

    @Test
    fun `maps command service internal failure paths`() {
        assertApiException(
            expectedStatus = HttpStatus.CONFLICT,
            expectedCode = "rule_management_event_replay_failed",
            expectedDetail = "Stored rule management events could not be replayed.",
            actual =
                RuleManagementCommandServiceError
                    .EventReplayFailed(RuleManagementEventApplyError.DraftNotFound(draftId()))
                    .toApiException(),
        )
        assertApiException(
            expectedStatus = HttpStatus.CONFLICT,
            expectedCode = "rule_management_event_replay_failed",
            expectedDetail = "Stored rule management events could not be replayed.",
            actual =
                RuleManagementCommandServiceError
                    .CommandProcessFailed(
                        RuleManagementProcessError.EventApplyFailed(
                            RuleManagementEventApplyError.DraftNotFound(draftId()),
                        ),
                    ).toApiException(),
        )
        assertApiException(
            expectedStatus = HttpStatus.CONFLICT,
            expectedCode = "stream_version_conflict",
            expectedDetail = "Rule management stream version does not match the expected version.",
            actual =
                RuleManagementCommandServiceError
                    .EventStoreAppendFailed(
                        RuleManagementEventStoreError.StreamVersionConflict(
                            draftId = draftId(),
                            expected = RuleManagementExpectedStreamVersion.NoStream,
                            actual = null,
                        ),
                    ).toApiException(),
        )
    }

    @Test
    fun `maps direct store and value object errors`() {
        assertApiException(
            expectedStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            expectedCode = "rule_management_storage_failure",
            expectedDetail = "database unavailable",
            actual = RuleManagementEventStoreError.StorageFailure("database unavailable").toApiException(),
        )

        assertEquals("invalid_rule_set_version", ValueObjectException(ValueObjectError.NotPositive("ruleSetVersion", 0)).code)
        assertEquals(
            "invalid_rule_management_stream_version",
            ValueObjectException(ValueObjectError.NotPositive("ruleManagementStreamVersion", 0)).code,
        )
        assertEquals(
            "invalid_rule_set_draft_id",
            ValueObjectException(ValueObjectError.UnsupportedUuidVersion("ruleSetDraftId", 4)).code,
        )
    }

    private fun assertApiException(
        expectedStatus: HttpStatus,
        expectedCode: String,
        expectedDetail: String,
        actual: me.sensibile.kopringbricks.web.problem.autoconfigure.ApiException,
    ) {
        assertEquals(expectedStatus, actual.statusCode)
        assertEquals(expectedCode, actual.code)
        assertEquals(expectedDetail, actual.body.detail)
    }

    private fun draftId(value: String = DRAFT_ID): RuleSetDraftId =
        when (val result = RuleSetDraftId.of(Uuid.parse(value))) {
            is Outcome.Err -> error("Invalid test draft id: ${result.error}")
            is Outcome.Ok -> result.value
        }

    private fun ruleId(): RuleId =
        when (val result = RuleId.of(Uuid.parse(RULE_ID))) {
            is Outcome.Err -> error("Invalid test rule id: ${result.error}")
            is Outcome.Ok -> result.value
        }

    private fun flagKey(value: String): FlagKey =
        when (val result = FlagKey.of(value)) {
            is Outcome.Err -> error("Invalid test flag key: ${result.error}")
            is Outcome.Ok -> result.value
        }
}

private const val DRAFT_ID = "018ff7c1-9354-7b02-b021-76d2791d6a21"
private const val SECOND_DRAFT_ID = "018ff7c1-9354-7b02-b021-76d2791d6a24"
private const val RULE_ID = "018ff7c1-9354-7b02-b021-76d2791d6a23"
