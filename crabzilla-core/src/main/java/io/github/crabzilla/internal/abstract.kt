package io.github.crabzilla.internal

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.CommandMetadata
import io.github.crabzilla.core.Entity
import io.github.crabzilla.core.Snapshot
import io.github.crabzilla.core.UnitOfWork
import io.github.crabzilla.core.Version
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import java.util.UUID

interface EntityComponent<E : Entity> {
  fun entityName(): String
  fun handleCommand(metadata: CommandMetadata, command: Command): Future<Pair<UnitOfWork, Long>>
  fun getUowByUowId(uowId: Long): Future<UnitOfWork>
  fun getAllUowByEntityId(id: Int): Future<List<UnitOfWork>>
  fun getSnapshot(entityId: Int): Future<Snapshot<E>>
  fun toJson(state: E): JsonObject
  fun cmdFromJson(commandName: String, cmdAsJson: JsonObject): Command
}

interface UnitOfWorkJournal {
  fun append(unitOfWork: UnitOfWork): Future<Long>
}

interface UnitOfWorkRepository {
  fun getUowByCmdId(cmdId: UUID): Future<Pair<UnitOfWork, Long>>
  fun getUowByUowId(uowId: Long): Future<UnitOfWork>
  fun selectAfterVersion(id: Int, version: Version, aggregateRootName: String): Future<RangeOfEvents>
  fun selectAfterUowId(uowId: Long, maxRows: Int): Future<List<UnitOfWorkEvents>>
  fun getAllUowByEntityId(id: Int): Future<List<UnitOfWork>>
}
