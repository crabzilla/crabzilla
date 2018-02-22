package io.github.crabzilla.example1


import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.example1.customer.CustomerActivated
import io.github.crabzilla.example1.customer.CustomerCreated
import io.github.crabzilla.example1.customer.CustomerDeactivated
import io.github.crabzilla.vertx.projector.AbstractEventsProjector
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

// tag::projector[]
class CustomerSummaryProjector(channelId: String, jdbi: Jdbi,
                               daoFactory: (Handle, Class<CustomerSummaryProjectorDao>) -> CustomerSummaryProjectorDao)

  : AbstractEventsProjector<CustomerSummaryProjectorDao>(channelId, jdbi, CustomerSummaryProjectorDao::class.java, daoFactory) {

  companion object {
    private val log = LoggerFactory.getLogger(CustomerSummaryProjector::class.java.simpleName)
  }

  override fun write(dao: CustomerSummaryProjectorDao, targetId: String, event: DomainEvent) {

    log.info("event {} from channel {}", event, eventsChannelId)

    when (event) {
      is CustomerCreated -> dao.insert(CustomerSummary(targetId, event.name, false))
      is CustomerActivated -> dao.updateStatus(targetId, true)
      is CustomerDeactivated -> dao.updateStatus(targetId, false)
      else -> log.info("${event.javaClass.simpleName} does not have any event projector handler")
    }
  }
}
// end::projector[]
