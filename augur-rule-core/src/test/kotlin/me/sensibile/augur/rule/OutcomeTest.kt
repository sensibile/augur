package me.sensibile.augur.rule

import kotlin.test.Test
import kotlin.test.assertEquals

class OutcomeTest {
    @Test
    fun `map transforms ok value`() {
        val actual = Outcome.Ok(1).map { it + 1 }

        assertEquals(Outcome.Ok(2), actual)
    }

    @Test
    fun `flatMap preserves err value`() {
        val actual = Outcome.Err("invalid").flatMap { value: Int -> Outcome.Ok(value + 1) }

        assertEquals(Outcome.Err("invalid"), actual)
    }
}
