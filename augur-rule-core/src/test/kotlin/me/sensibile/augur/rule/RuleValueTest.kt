package me.sensibile.augur.rule

import kotlin.test.Test
import kotlin.test.assertEquals

class RuleValueTest {
    @Test
    fun `number rejects non finite values`() {
        val actual = RuleValue.number(Double.NaN)

        assertEquals(Outcome.Err(ValueObjectError.NotFinite("ruleValue", Double.NaN)), actual)
    }
}
