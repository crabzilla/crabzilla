package io.github.crabzilla.framework

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject

interface EntityComponent<E: Entity> {

  fun entityName() : String

  fun handleCommand(metadata: CommandMetadata, command: Command, aHandler: Handler<AsyncResult<Pair<UnitOfWork, Long>>>)

  fun getUowByUowId(uowId: Long, aHandler: Handler<AsyncResult<UnitOfWork>>)

  fun getAllUowByEntityId(id: Int, aHandler: Handler<AsyncResult<List<UnitOfWork>>>)

  fun getSnapshot(entityId: Int, aHandler: Handler<AsyncResult<Snapshot<E>>>)

  fun toJson(state: E): JsonObject

  fun cmdFromJson(commandName: String, cmdAsJson: JsonObject): Command

}

