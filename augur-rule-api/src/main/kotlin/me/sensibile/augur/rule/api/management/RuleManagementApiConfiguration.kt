@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package me.sensibile.augur.rule.api.management

import me.sensibile.augur.rule.Outcome
import me.sensibile.augur.rule.eventsourcing.BricksRuleManagementEventStore
import me.sensibile.augur.rule.eventsourcing.InMemoryBricksEventStore
import me.sensibile.augur.rule.management.RuleManagementCommandService
import me.sensibile.augur.rule.management.RuleManagementEventId
import me.sensibile.augur.rule.management.RuleManagementEventIdGenerator
import me.sensibile.augur.rule.management.RuleManagementEventStore
import me.sensibile.augur.rule.management.RuleSetDraftId
import me.sensibile.augur.rule.management.RuleSetDraftIdGenerator
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventSourcingTemplate
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventStore
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.uuid.Uuid

@Configuration
class RuleManagementApiConfiguration {
    @Bean
    @ConditionalOnMissingBean(EventStore::class)
    @ConditionalOnProperty(
        prefix = "augur.rule-management.event-store.in-memory",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true,
    )
    fun eventStore(): EventStore = InMemoryBricksEventStore()

    @Bean
    fun ruleManagementEventStore(events: EventSourcingTemplate): RuleManagementEventStore = BricksRuleManagementEventStore(events)

    @Bean
    fun ruleManagementCommandService(eventStore: RuleManagementEventStore): RuleManagementCommandService =
        RuleManagementCommandService(eventStore)

    @Bean
    fun ruleSetDraftIdGenerator(): RuleSetDraftIdGenerator =
        object : RuleSetDraftIdGenerator {
            override fun nextRuleSetDraftId(): RuleSetDraftId = RuleSetDraftId.of(Uuid.generateV7()).requireOk()
        }

    @Bean
    fun ruleManagementEventIdGenerator(): RuleManagementEventIdGenerator =
        object : RuleManagementEventIdGenerator {
            override fun nextRuleManagementEventId(): RuleManagementEventId = RuleManagementEventId.of(Uuid.generateV7()).requireOk()
        }
}

private fun <E, A> Outcome<E, A>.requireOk(): A =
    when (this) {
        is Outcome.Err -> error("Generated value failed validation: $error")
        is Outcome.Ok -> value
    }
