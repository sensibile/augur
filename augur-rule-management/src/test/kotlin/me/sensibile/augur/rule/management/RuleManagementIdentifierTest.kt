@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package me.sensibile.augur.rule.management

import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.ValueObjectError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.uuid.Uuid

class RuleManagementIdentifierTest {
    @Test
    fun `draft id accepts uuid v7`() {
        val uuid = Uuid.parse("018ff7c1-9354-7b02-b021-76d2791d6a21")

        val actual = RuleSetDraftId.of(uuid)

        assertEquals(Outcome.Ok(uuid.unsafeDraftId()), actual)
    }

    @Test
    fun `draft id rejects non uuid v7`() {
        val actual = RuleSetDraftId.of(Uuid.parse("550e8400-e29b-41d4-a716-446655440000"))

        assertEquals(
            Outcome.Err(ValueObjectError.UnsupportedUuidVersion("ruleSetDraftId", 4)),
            actual,
        )
    }

    @Test
    fun `published rule set id rejects non uuid v7`() {
        val actual = PublishedRuleSetId.of(Uuid.parse("550e8400-e29b-41d4-a716-446655440000"))

        assertIs<Outcome.Err<ValueObjectError.UnsupportedUuidVersion>>(actual)
    }

    @Test
    fun `event id rejects non uuid v7`() {
        val actual = RuleManagementEventId.of(Uuid.parse("550e8400-e29b-41d4-a716-446655440000"))

        assertIs<Outcome.Err<ValueObjectError.UnsupportedUuidVersion>>(actual)
    }

    private fun Uuid.unsafeDraftId(): RuleSetDraftId =
        when (val draftId = RuleSetDraftId.of(this)) {
            is Outcome.Err -> error("Invalid test draft id: $this")
            is Outcome.Ok -> draftId.value
        }
}
