package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.eventsourcing.BricksRuleManagementEventStore
import me.sensibile.augur.rule.eventsourcing.InMemoryBricksEventStore
import me.sensibile.augur.rule.management.RuleManagementCommandService
import me.sensibile.augur.rule.management.RuleManagementEventStore
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventSourcingTemplate
import kotlin.test.Test
import kotlin.test.assertIs

class RuleManagementApiConfigurationTest {
    private val configuration = RuleManagementApiConfiguration()

    @Test
    fun `provides in memory bricks event store fallback`() {
        val actual = configuration.eventStore()

        assertIs<InMemoryBricksEventStore>(actual)
    }

    @Test
    fun `adapts bricks event sourcing template to rule management event store port`() {
        val bricksStore = InMemoryBricksEventStore()
        val actual = configuration.ruleManagementEventStore(EventSourcingTemplate(bricksStore))

        assertIs<BricksRuleManagementEventStore>(actual)
    }

    @Test
    fun `wires command service to rule management event store port`() {
        val eventStore: RuleManagementEventStore =
            configuration.ruleManagementEventStore(EventSourcingTemplate(InMemoryBricksEventStore()))
        val actual = configuration.ruleManagementCommandService(eventStore)

        assertIs<RuleManagementCommandService>(actual)
    }
}
