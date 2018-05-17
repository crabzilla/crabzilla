package io.github.crabzilla.vertx.projector

import io.github.crabzilla.core.DomainEvent

// TODO https://streamdata.io/blog/vert-x-and-the-async-calls-chain/

interface EventsProjector<DAO> {

  val eventsChannelId: String
  val daoClass: Class<DAO>

  fun handle(uowList: List<ProjectionData>)

  fun write(dao: DAO, targetId: String, event: DomainEvent)

}
