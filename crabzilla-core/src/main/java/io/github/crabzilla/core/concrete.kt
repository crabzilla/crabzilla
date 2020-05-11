package io.github.crabzilla.core

import io.github.crabzilla.internal.CommandController
import io.github.crabzilla.internal.EntityComponent
import io.github.crabzilla.internal.UnitOfWorkJournal
import io.github.crabzilla.internal.UnitOfWorkRepository
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.core.shareddata.SharedData
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

val ENTITY_SERIALIZER = PolymorphicSerializer(Entity::class)
val COMMAND_SERIALIZER = PolymorphicSerializer(Command::class)
val EVENT_SERIALIZER = PolymorphicSerializer(DomainEvent::class)

typealias Version = Int

typealias CommandContext<E> = Triple<CommandMetadata, Command, Snapshot<E>>

data class CommandMetadata(
  val entityId: Int,
  val entityName: String,
  val commandName: String,
  val commandId: UUID = UUID.randomUUID()
)

data class Snapshot<E : Entity>(
  val state: E,
  val version: Version
)

class StateTransitionsTracker<A : Entity>(originalState: A, private val stateFn: (DomainEvent, A) -> A) {
  val appliedEvents = mutableListOf<DomainEvent>()
  var currentState: A = originalState

  fun applyEvents(events: List<DomainEvent>): StateTransitionsTracker<A> {
    events.forEach { domainEvent ->
      currentState = stateFn.invoke(domainEvent, currentState)
      appliedEvents.add(domainEvent)
    }
    return this
  }

  inline fun applyEvents(fn: (A) -> List<DomainEvent>): StateTransitionsTracker<A> {
    val newEvents = fn.invoke(currentState)
    return applyEvents(newEvents)
  }
}

data class UnitOfWork(
  val entityName: String,
  val entityId: Int,
  val commandId: UUID,
  val command: Command,
  val version: Version,
  val events: List<DomainEvent>
) {
  init {
    require(this.version >= 1) { "version must be >= 1" }
  }

  object JsonMetadata {
    const val ENTITY_NAME = "entityName"
    const val ENTITY_ID = "entityId"
    const val COMMAND_ID = "commandId"
    const val COMMAND = "command"
    const val VERSION = "version"
    const val EVENTS = "events"
  }
}

/**
 * A light implementation
 */
class InMemorySnapshotRepository<E : Entity>(
  private val sharedData: SharedData,
  private val json: Json,
  private val entityName: String,
  private val initialState: E
) : SnapshotRepository<E> {
  companion object {
    internal val log = LoggerFactory.getLogger(CommandController::class.java)
  }
  override fun upsert(entityId: Int, snapshot: Snapshot<E>): Future<Void> {
    val promise = Promise.promise<Void>()
    sharedData.getAsyncMap<Int, String>(entityName) { event1 ->
      if (event1.failed()) {
        log.error("Failed to get map $entityName")
        promise.fail(event1.cause())
        return@getAsyncMap
      }
      val stateAsJson = JsonObject(json.stringify(ENTITY_SERIALIZER, snapshot.state))
      val mapEntryAsJson = JsonObject().put("version", snapshot.version).put("state", stateAsJson)
      event1.result().put(entityId, mapEntryAsJson.encode()) { event2 ->
        if (event2.failed()) {
          log.error("Failed to put $entityId on map $entityName")
          promise.fail(event2.cause())
          return@put
        }
        promise.complete()
      }
    }
    return promise.future()
  }

  override fun retrieve(entityId: Int): Future<Snapshot<E>> {
    val promise = Promise.promise<Snapshot<E>>()
    val defaultSnapshot = Snapshot(initialState, 0)
    sharedData.getAsyncMap<Int, String>(entityName) { event1 ->
      if (event1.failed()) {
        log.error("Failed get map $entityName")
        promise.fail(event1.cause())
        return@getAsyncMap
      }
      event1.result().get(entityId) { event2 ->
        if (event2.failed()) {
          log.error("Failed to get $entityId on map $entityName")
          promise.fail(event2.cause())
          return@get
        }
        val result = event2.result()
        if (result == null) {
          if (log.isDebugEnabled) {
            log.debug("Returning default snapshot for $entityId on map $entityName")
          }
          promise.complete(defaultSnapshot)
          return@get
        }
        val mapEntryAsJson = JsonObject(event2.result())
        val version = mapEntryAsJson.getInteger("version")
        val stateAsJson = mapEntryAsJson.getJsonObject("state")
        val state = json.parse(ENTITY_SERIALIZER, stateAsJson.encode()) as E
        promise.complete(Snapshot(state, version))
      }
    }
    return promise.future()
  }
}

data class UnitOfWorkEvents(val uowId: Long, val entityId: Int, val events: List<DomainEvent>)

class CrabzillaContext(
  val json: Json,
  val uowRepository: UnitOfWorkRepository,
  val uowJournal: UnitOfWorkJournal
)

class WebResourceContext<E : Entity>(
  val resourceName: String,
  val entityName: String,
  val cmdTypeMap: Map<String, String>,
  val cmdAware: EntityCommandAware<E>,
  val snapshotRepo: SnapshotRepository<E>
)

class EntityComponent<E : Entity>(
  private val ctx: CrabzillaContext,
  private val entityName: String,
  private val snapshotRepo: SnapshotRepository<E>,
  cmdAware: EntityCommandAware<E>
) : EntityComponent<E> {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(EntityComponent::class.java)
  }

  private val cmdController = CommandController(cmdAware, snapshotRepo, ctx.uowJournal)

  override fun entityName(): String {
    return entityName
  }

  override fun getUowByUowId(uowId: Long): Future<UnitOfWork> {
    return ctx.uowRepository.getUowByUowId(uowId)
  }

  override fun getAllUowByEntityId(id: Int): Future<List<UnitOfWork>> {
    return ctx.uowRepository.getAllUowByEntityId(id)
  }

  override fun getSnapshot(entityId: Int): Future<Snapshot<E>> {
    return snapshotRepo.retrieve(entityId)
  }

  override fun handleCommand(metadata: CommandMetadata, command: Command): Future<Pair<UnitOfWork, Long>> {
    val promise = Promise.promise<Pair<UnitOfWork, Long>>()
    ctx.uowRepository.getUowByCmdId(metadata.commandId).onComplete { gotCommand ->
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
          if (log.isDebugEnabled) log.debug("Command successfully handled: $pair")
        } else {
          log.error("When handling command", cmdHandled.cause())
          promise.fail(cmdHandled.cause())
        }
      }
    }
    return promise.future()
  }

  override fun toJson(state: E): JsonObject {
    return JsonObject(ctx.json.stringify(ENTITY_SERIALIZER, state))
  }

  override fun cmdFromJson(commandName: String, cmdAsJson: JsonObject): Command {
    return ctx.json.parse(COMMAND_SERIALIZER, cmdAsJson.encode())
  }
}
