package crabzilla.example1


import crabzilla.example1.customer.CustomerActivated
import crabzilla.example1.customer.CustomerCreated
import crabzilla.example1.customer.CustomerDeactivated
import crabzilla.model.DomainEvent
import crabzilla.stack.EventProjector
import org.jdbi.v3.core.Jdbi

class KExample1EventProjector(channelId: String, daoClazz: Class<CustomerSummaryDao>, jdbi: Jdbi)

  : EventProjector<CustomerSummaryDao>(channelId, daoClazz, jdbi) {

  //val log = KotlinLogging.logger {}

  override fun write(dao: CustomerSummaryDao, targetId: String, event: DomainEvent) {

    //log.info("event {} from channel {}", event, eventsChannelId)

    when (event) {
      is CustomerCreated -> dao.insert(CustomerSummary(targetId, event.name, false))
      is CustomerActivated -> dao.updateStatus(targetId, true)
      is CustomerDeactivated -> dao.updateStatus(targetId, false)
      else -> println("${event.javaClass.simpleName} does not have any event projection handler")
    }
  }
}
