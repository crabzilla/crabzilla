package io.github.crabzilla

import io.vertx.core.json.JsonObject

data class UnitOfWorkEvents(val uowId: Long, val entityId: Int,
                            val events: List<Pair<String, DomainEvent>>)

fun fromUnitOfWork(uowId: Long, uow: UnitOfWork) : UnitOfWorkEvents {
  return UnitOfWorkEvents(uowId, uow.entityId, uow.events)
}

fun fromJsonObject(json: JsonObject) {
  return
}
