@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package me.sensibile.augur.rule

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.uuid.Uuid

class IdentifierTest {
    @Test
    fun `flag key accepts normalized lower case keys`() {
        val actual = FlagKey.of(" checkout.new-flow ")

        assertEquals(Outcome.Ok("checkout.new-flow".unsafeFlagKey()), actual)
    }

    @Test
    fun `flag key rejects blank key`() {
        val actual = FlagKey.of(" ")

        assertEquals(Outcome.Err(ValueObjectError.Blank("flagKey")), actual)
    }

    @Test
    fun `rule id rejects non uuid v7`() {
        val actual = RuleId.of(Uuid.parse("550e8400-e29b-41d4-a716-446655440000"))

        assertIs<Outcome.Err<ValueObjectError.UnsupportedUuidVersion>>(actual)
    }

    @Test
    fun `rule id generates uuid v7`() {
        val actual = RuleId.generate()

        assertIs<RuleId>(actual)
    }

    @Test
    fun `rule set version must be positive`() {
        val actual = RuleSetVersion.of(0)

        assertEquals(Outcome.Err(ValueObjectError.NotPositive("ruleSetVersion", 0)), actual)
    }

    private fun String.unsafeFlagKey(): FlagKey =
        when (val key = FlagKey.of(this)) {
            is Outcome.Err -> error("Invalid test flag key: $this")
            is Outcome.Ok -> key.value
        }
}
