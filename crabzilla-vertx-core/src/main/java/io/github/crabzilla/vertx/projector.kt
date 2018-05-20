package io.github.crabzilla.vertx

import io.github.crabzilla.core.DomainEvent
import java.util.*

interface EventsProjector<DAO> {

  val eventsChannelId: String
  val daoClass: Class<DAO>

  fun handle(uowList: List<ProjectionData>)

  fun write(dao: DAO, targetId: String, event: DomainEvent)

}

data class ProjectionData(val uowId: UUID, val uowSequence: Long,
                          val targetId: String, val events: List<DomainEvent>)

