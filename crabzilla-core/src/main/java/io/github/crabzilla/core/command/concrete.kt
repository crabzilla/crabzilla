package io.github.crabzilla.core.command

import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.core.shareddata.SharedData
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

object EventBusChannels {
  val aggregateRootChannel = { entityName: String -> "crabzilla.aggregate.$entityName" }
}

val AGGREGATE_ROOT_SERIALIZER = PolymorphicSerializer(AggregateRoot::class)
val COMMAND_SERIALIZER = PolymorphicSerializer(Command::class)
val DOMAIN_EVENT_SERIALIZER = PolymorphicSerializer(DomainEvent::class)

typealias Version = Int

data class UnitOfWorkEvents(val uowId: Long, val entityId: Int, val events: List<DomainEvent>)

class CrabzillaContext(
  val json: Json,
  val uowRepository: UnitOfWorkRepository,
  val uowJournal: UnitOfWorkJournal
)

data class CommandMetadata(
  val aggregateRootId: Int,
  val entityName: String,
  val commandName: String,
  val commandId: UUID = UUID.randomUUID()
)

data class Snapshot<A : AggregateRoot>(
  val state: A,
  val version: Version
)

typealias CommandContext<A> = Triple<CommandMetadata, Command, Snapshot<A>>

data class UnitOfWork(
  val entityName: String,
  val aggregateRootId: Int,
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
    const val ENTITY_ID = "aggregateRootId"
    const val COMMAND_ID = "commandId"
    const val COMMAND = "command"
    const val VERSION = "version"
    const val EVENTS = "events"
  }
}

class StateTransitionsTracker<A : AggregateRoot>(originalState: A, private val stateFn: (DomainEvent, A) -> A) {
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

class CommandController<A : AggregateRoot>(
  private val commandAware: AggregateRootCommandAware<A>,
  private val snapshotRepo: SnapshotRepository<A>,
  private val uowJournal: UnitOfWorkJournal
) {
  companion object {
    internal val log = LoggerFactory.getLogger(CommandController::class.java)
  }
  fun handle(metadata: CommandMetadata, command: Command): Future<Pair<UnitOfWork, Long>> {
    fun toUnitOfWork(ctx: CommandContext<A>, events: List<DomainEvent>): UnitOfWork {
      val (cmdMetadata, _, snapshot) = ctx
      val (entityId, entityName, _, commandId) = cmdMetadata
      return UnitOfWork(entityName, entityId, commandId, command, snapshot.version + 1, events)
    }
    val promise = Promise.promise<Pair<UnitOfWork, Long>>()
    if (log.isDebugEnabled) log.debug("received $metadata\n $command")
    val constraints = commandAware.validateCmd(command)
    if (constraints.isNotEmpty()) {
      log.error("Command is invalid: $constraints")
      promise.fail(constraints.toString())
      return promise.future()
    }
    val snapshotValue: AtomicReference<Snapshot<A>> = AtomicReference()
    val uowValue: AtomicReference<UnitOfWork> = AtomicReference()
    val uowIdValue: AtomicReference<Long> = AtomicReference()
    snapshotRepo.retrieve(metadata.aggregateRootId)
      .compose { snapshot ->
        if (log.isDebugEnabled) log.debug("got snapshot $snapshot")
        val cachedSnapshot = snapshot ?: Snapshot(commandAware.initialState, 0)
        snapshotValue.set(cachedSnapshot)
        commandAware.handleCmd(metadata.aggregateRootId, cachedSnapshot.state, command)
      }
      .compose { eventsList ->
        val request = Triple(metadata, command, snapshotValue.get())
        val uow = toUnitOfWork(request, eventsList)
        if (log.isDebugEnabled) log.debug("got unitOfWork $uow")
        // append to journal
        uowValue.set(uow)
        uowJournal.append(uow)
      }
      .compose { uowId ->
        if (log.isDebugEnabled) log.debug("got uowId $uowId")
        uowIdValue.set(uowId)
        // compute new snapshot
        if (log.isDebugEnabled) log.debug("computing new snapshot")
        val newInstance = uowValue.get().events
          .fold(snapshotValue.get().state) { state, event -> commandAware.applyEvent.invoke(event, state) }
        val newSnapshot = Snapshot(newInstance, uowValue.get().version)
        if (log.isDebugEnabled) log.debug("now will store snapshot $newSnapshot")
        snapshotRepo.upsert(metadata.aggregateRootId, newSnapshot) // TODO what if only this side effect fail?
      }
      .onSuccess {
        val pair: Pair<UnitOfWork, Long> = Pair(uowValue.get(), uowIdValue.get())
        if (log.isDebugEnabled) log.debug("command handling success: $pair")
        promise.complete(pair)
      }.onFailure { err -> promise.fail(err) }

    return promise.future()
  }
}

class InMemorySnapshotRepository<A : AggregateRoot>(
  private val sharedData: SharedData, // TODO how to avoid to get the map on every time?
  private val json: Json,
  private val commandAware: AggregateRootCommandAware<A>
) : SnapshotRepository<A> {

  companion object {
    internal val log = LoggerFactory.getLogger(InMemorySnapshotRepository::class.java)
  }

  override fun upsert(id: Int, snapshot: Snapshot<A>): Future<Void> {
    val promise = Promise.promise<Void>()
    sharedData.getAsyncMap<Int, String>(commandAware.entityName) { event1 ->
      if (event1.failed()) {
        log.error("Failed to get map ${commandAware.entityName}")
        promise.fail(event1.cause())
        return@getAsyncMap
      }
      val stateAsJson = JsonObject(json.stringify(AGGREGATE_ROOT_SERIALIZER, snapshot.state))
      val mapEntryAsJson = JsonObject().put("version", snapshot.version).put("state", stateAsJson)
      event1.result().put(id, mapEntryAsJson.encode()) { event2 ->
        if (event2.failed()) {
          log.error("Failed to put $id on map ${commandAware.entityName}")
          promise.fail(event2.cause())
          return@put
        }
        promise.complete()
      }
    }
    return promise.future()
  }

  override fun retrieve(id: Int): Future<Snapshot<A>> {
    val promise = Promise.promise<Snapshot<A>>()
    val defaultSnapshot = Snapshot(commandAware.initialState, 0)
    sharedData.getAsyncMap<Int, String>(commandAware.entityName) { event1 ->
      if (event1.failed()) {
        log.error("Failed get map ${commandAware.entityName}")
        promise.fail(event1.cause())
        return@getAsyncMap
      }
      event1.result().get(id) { event2 ->
        if (event2.failed()) {
          log.error("Failed to get $id on map ${commandAware.entityName}")
          promise.fail(event2.cause())
          return@get
        }
        val result = event2.result()
        if (result == null) {
          if (log.isDebugEnabled) {
            log.debug("Returning default snapshot for $id on map ${commandAware.entityName}")
          }
          promise.complete(defaultSnapshot)
          return@get
        }
        val mapEntryAsJson = JsonObject(event2.result())
        val version = mapEntryAsJson.getInteger("version")
        val stateAsJson = mapEntryAsJson.getJsonObject("state")
        val state = json.parse(AGGREGATE_ROOT_SERIALIZER, stateAsJson.encode()) as A
        promise.complete(Snapshot(state, version))
      }
    }
    return promise.future()
  }
}
