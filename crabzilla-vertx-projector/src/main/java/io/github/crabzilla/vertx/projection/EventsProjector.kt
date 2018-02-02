package io.github.crabzilla.vertx.projection

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.vertx.ProjectionData
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory.getLogger

abstract class EventsProjector<in DAO>(protected val eventsChannelId: String,
                                       private val jdbi: Jdbi,
                                       private val daoClass: Class<DAO>,
                                       private val daoFactory: (Handle, Class<DAO>) -> DAO) {
  fun handle(uowList: List<ProjectionData>) {

    log.info("Writing {} units for eventChannel {}", uowList.size, eventsChannelId)

    val h = jdbi.open()

    try {
      h.begin()
      val dao = daoFactory.invoke(h, daoClass)
      uowList.flatMap { (_, _, targetId, events) -> events.map { Pair(targetId, it) }}
        .forEach { (id, event) -> write(dao, id, event) }
      h.commit()
    } catch (e:  Exception) {
      h.rollback()
      log.error("When projection events to db", e)
    } finally {
      h.close()
    }

  }

  abstract fun write(dao: DAO, targetId: String, event: DomainEvent)

  companion object {

    private val log = getLogger(EventsProjector::class.java)
  }

}
