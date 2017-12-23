package io.github.crabzilla.example1


import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.example1.customer.CustomerActivated
import io.github.crabzilla.example1.customer.CustomerCreated
import io.github.crabzilla.example1.customer.CustomerDeactivated
import io.github.crabzilla.vertx.projection.EventProjector
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

// tag::projector[]
class Example1EventProjector(channelId: String, daoClazz: Class<CustomerSummaryDao>, jdbi: Jdbi)

  : EventProjector<CustomerSummaryDao>(channelId, daoClazz, jdbi) {

  val log = LoggerFactory.getLogger(Example1EventProjector::class.java.simpleName)

  override fun write(dao: CustomerSummaryDao, targetId: String, event: DomainEvent) {

    log.info("event {} from channel {}", event, eventsChannelId)

    when (event) {
      is CustomerCreated -> dao.insert(CustomerSummary(targetId, event.name, false))
      is CustomerActivated -> dao.updateStatus(targetId, true)
      is CustomerDeactivated -> dao.updateStatus(targetId, false)
      else -> println("${event.javaClass.simpleName} does not have any event projection handler")
    }
  }
}
// end::projector[]
