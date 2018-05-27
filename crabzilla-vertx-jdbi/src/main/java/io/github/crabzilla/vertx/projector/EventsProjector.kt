package io.github.crabzilla.vertx.projector

import io.github.crabzilla.DomainEvent
import io.github.crabzilla.vertx.ProjectionData

interface EventsProjector<DAO> {

  val eventsChannelId: String
  val daoClass: Class<DAO>

  fun handle(uowList: List<ProjectionData>)

  fun write(dao: DAO, targetId: String, event: DomainEvent)

}
