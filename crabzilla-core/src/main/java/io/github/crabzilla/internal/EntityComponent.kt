package io.github.crabzilla.internal

import io.github.crabzilla.framework.*
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject

interface EntityComponent<E: Entity> {

  fun entityName() : String

  fun handleCommand(metadata: CommandMetadata, command: Command) : Promise<Pair<UnitOfWork, Long>>

  fun getUowByUowId(uowId: Long) : Promise<UnitOfWork>

  fun getAllUowByEntityId(id: Int) : Promise<List<UnitOfWork>>

  fun getSnapshot(entityId: Int) : Promise<Snapshot<E>>

  fun toJson(state: E): JsonObject

  fun cmdFromJson(commandName: String, cmdAsJson: JsonObject): Command

}
