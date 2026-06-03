package me.sensibile.augur.rule.management

import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.ValueObjectError
import kotlin.test.Test
import kotlin.test.assertEquals

class RuleManagementEventStoreTest {
    @Test
    fun `creates stream version from positive value`() {
        val actual = RuleManagementStreamVersion.of(1)

        assertEquals(Outcome.Ok(streamVersion(1)), actual)
    }

    @Test
    fun `rejects non positive stream version`() {
        val actual = RuleManagementStreamVersion.of(0)

        assertEquals(Outcome.Err(ValueObjectError.NotPositive("ruleManagementStreamVersion", 0)), actual)
    }

    @Test
    fun `models empty expected stream version separately from rule set version`() {
        val expected = RuleManagementExpectedStreamVersion.NoStream

        assertEquals(RuleManagementExpectedStreamVersion.NoStream, expected)
    }

    @Test
    fun `models exact expected stream version separately from rule set version`() {
        val version = streamVersion(1)
        val expected = RuleManagementExpectedStreamVersion.Exact(version)

        assertEquals(version, expected.version)
    }
}
