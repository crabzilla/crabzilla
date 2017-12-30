package io.github.crabzilla.vertx.projection

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.vertx.ProjectionData
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory.getLogger

abstract class EventsProjector<in DAO>(protected val eventsChannelId: String,
                                       private val jdbi: Jdbi,
                                       private val daoFactory: (jdbi: Jdbi) -> DAO) {
  /**
   * TODO try to use 1 transaction for all events (db with auto-commit = false)
   * @param uowList
   */
  fun handle(uowList: List<ProjectionData>) {
    log.info("Writing {} units for eventChannel {}", uowList.size, eventsChannelId)

    val dao = daoFactory.invoke(jdbi)

    uowList.flatMap { (_, _, targetId, events) -> events.map { e -> Pair(targetId, e) }}
            .forEach { (id, event) -> write(dao, id, event) }

//    jdbi.inTransactionUnchecked { h ->
//    val dao = daoFactory.invoke(jdbi)
//      uowList.flatMap { (_, _, targetId, events) -> events.map { Pair(targetId, it) }}
//              .forEach { (id, event) -> write(dao, id, event) }
//    }

  }

  abstract fun write(dao: DAO, targetId: String, event: DomainEvent)

  companion object {

    private val log = getLogger(EventsProjector::class.java)
  }

}
