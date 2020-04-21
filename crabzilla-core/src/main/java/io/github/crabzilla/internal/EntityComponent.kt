package io.github.crabzilla.internal

import io.github.crabzilla.framework.Command
import io.github.crabzilla.framework.CommandMetadata
import io.github.crabzilla.framework.Entity
import io.github.crabzilla.framework.Snapshot
import io.github.crabzilla.framework.UnitOfWork
import io.vertx.core.Future
import io.vertx.core.json.JsonObject

interface EntityComponent<E : Entity> {
  fun entityName(): String
  fun handleCommand(metadata: CommandMetadata, command: Command): Future<Pair<UnitOfWork, Long>>
  fun getUowByUowId(uowId: Long): Future<UnitOfWork>
  fun getAllUowByEntityId(id: Int): Future<List<UnitOfWork>>
  fun getSnapshot(entityId: Int): Future<Snapshot<E>>
  fun toJson(state: E): JsonObject
  fun cmdFromJson(commandName: String, cmdAsJson: JsonObject): Command
}
