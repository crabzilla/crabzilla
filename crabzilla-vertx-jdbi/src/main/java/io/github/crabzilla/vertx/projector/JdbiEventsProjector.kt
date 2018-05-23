package io.github.crabzilla.vertx.projector

import io.github.crabzilla.vertx.ProjectionData
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory.getLogger

abstract class JdbiEventsProjector<DAO>(override val eventsChannelId: String,
                                        private val jdbi: Jdbi,
                                        override val daoClass: Class<DAO>,
                                        val daoFactory: (Handle, Class<DAO>) -> DAO) : EventsProjector<DAO> {
  override fun handle(uowList: List<ProjectionData>) {

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
      log.error("When projecting events to db", e)
    } finally {
      h.close()
    }

  }

  companion object {

    private val log = getLogger(JdbiEventsProjector::class.java)
  }

}
