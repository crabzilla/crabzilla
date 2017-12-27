package io.github.crabzilla.example1


import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.example1.customer.CustomerActivated
import io.github.crabzilla.example1.customer.CustomerCreated
import io.github.crabzilla.example1.customer.CustomerDeactivated
import io.github.crabzilla.vertx.projection.EventsProjector
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

// tag::projector[]
class CustomerSummaryProjector(channelId: String, projectorDaoClazz: Class<CustomerSummaryProjectorDao>, jdbi: Jdbi)

  : EventsProjector<CustomerSummaryProjectorDao>(channelId, projectorDaoClazz, jdbi) {

  private val log = LoggerFactory.getLogger(EventsProjector::class.java.simpleName)

  override fun write(projectorDao: CustomerSummaryProjectorDao, targetId: String, event: DomainEvent) {

    log.info("event {} from channel {}", event, eventsChannelId)

    when (event) {
      is CustomerCreated -> projectorDao.insert(CustomerSummary(targetId, event.name, false))
      is CustomerActivated -> projectorDao.updateStatus(targetId, true)
      is CustomerDeactivated -> projectorDao.updateStatus(targetId, false)
      else -> println("${event.javaClass.simpleName} does not have any event projection handler")
    }
  }
}
// end::projector[]
