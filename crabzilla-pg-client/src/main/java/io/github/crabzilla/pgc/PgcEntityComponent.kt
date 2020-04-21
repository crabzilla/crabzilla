package io.github.crabzilla.pgc

import io.github.crabzilla.framework.COMMAND_SERIALIZER
import io.github.crabzilla.framework.Command
import io.github.crabzilla.framework.CommandMetadata
import io.github.crabzilla.framework.ENTITY_SERIALIZER
import io.github.crabzilla.framework.Entity
import io.github.crabzilla.framework.EntityCommandAware
import io.github.crabzilla.framework.Snapshot
import io.github.crabzilla.framework.UnitOfWork
import io.github.crabzilla.internal.CommandController
import io.github.crabzilla.internal.EntityComponent
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PgcEntityComponent<E : Entity>(
  vertx: Vertx,
  writeDb: PgPool,
  private val json: Json,
  private val entityName: String,
  cmdAware: EntityCommandAware<E>
) : EntityComponent<E> {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(PgcEntityComponent::class.java)
  }
  private val uowRepo = PgcUowRepo(writeDb, json)
  private val snapshotRepo = PgcSnapshotRepo(writeDb, json, entityName, cmdAware)
  private val uowJournal = PgcUowJournal(vertx, writeDb, json)
  private val cmdController = CommandController(cmdAware, snapshotRepo, uowJournal)
  override fun entityName(): String {
    return entityName
  }
  override fun getUowByUowId(uowId: Long): Future<UnitOfWork> {
    return uowRepo.getUowByUowId(uowId)
  }
  override fun getAllUowByEntityId(id: Int): Future<List<UnitOfWork>> {
    return uowRepo.getAllUowByEntityId(id)
  }
  override fun getSnapshot(entityId: Int): Future<Snapshot<E>> {
    return snapshotRepo.retrieve(entityId)
  }
  override fun handleCommand(metadata: CommandMetadata, command: Command): Future<Pair<UnitOfWork, Long>> {
    val promise = Promise.promise<Pair<UnitOfWork, Long>>()
    uowRepo.getUowByCmdId(metadata.commandId).onComplete { gotCommand ->
      if (gotCommand.succeeded()) {
        val uowPair = gotCommand.result()
        if (uowPair != null) {
          promise.complete(uowPair)
          return@onComplete
        }
      }
      cmdController.handle(metadata, command).onComplete { cmdHandled ->
        if (cmdHandled.succeeded()) {
          val pair = cmdHandled.result()
          promise.complete(pair)
          if (log.isTraceEnabled) log.trace("Command successfully handled: $pair")
        } else {
          log.error("When handling command", cmdHandled.cause())
          promise.fail(cmdHandled.cause())
        }
      }
    }
    return promise.future()
  }
  override fun toJson(state: E): JsonObject {
    return JsonObject(json.stringify(ENTITY_SERIALIZER, state))
  }
  override fun cmdFromJson(commandName: String, cmdAsJson: JsonObject): Command {
    return json.parse(COMMAND_SERIALIZER, cmdAsJson.encode())
  }
}
