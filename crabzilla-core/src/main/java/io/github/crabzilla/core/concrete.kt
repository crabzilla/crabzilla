package io.github.crabzilla.core

import io.vertx.core.Future
import io.vertx.core.Promise
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

object EventBusChannels {
  const val unitOfWorkChannel = "crabzilla.events.channel"
}

val ENTITY_SERIALIZER = PolymorphicSerializer(Entity::class)
val COMMAND_SERIALIZER = PolymorphicSerializer(Command::class)
val EVENT_SERIALIZER = PolymorphicSerializer(DomainEvent::class)

typealias Version = Int

data class UnitOfWorkEvents(val uowId: Long, val entityId: Int, val events: List<DomainEvent>)

class CrabzillaContext(
  val json: Json,
  val uowRepository: UnitOfWorkRepository,
  val uowJournal: UnitOfWorkJournal
)

data class RangeOfEvents(val afterVersion: Version, val untilVersion: Version, val events: List<DomainEvent>)

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

object CrabzillaInternal {

  class CommandController<E : Entity>(
    private val commandAware: EntityCommandAware<E>,
    private val snapshotRepo: SnapshotRepository<E>,
    private val uowJournal: UnitOfWorkJournal
  ) {
    companion object {
      internal val log = LoggerFactory.getLogger(CommandController::class.java)
    }
    fun handle(metadata: CommandMetadata, command: Command): Future<Pair<UnitOfWork, Long>> {
      fun toUnitOfWork(ctx: CommandContext<E>, events: List<DomainEvent>): UnitOfWork {
        val (cmdMetadata, command, snapshot) = ctx
        return UnitOfWork(cmdMetadata.entityName, cmdMetadata.entityId, cmdMetadata.commandId,
          command, snapshot.version + 1, events)
      }
      val promise = Promise.promise<Pair<UnitOfWork, Long>>()
      if (log.isDebugEnabled) log.debug("received $metadata\n $command")
      val constraints = commandAware.validateCmd(command)
      if (constraints.isNotEmpty()) {
        log.error("Command is invalid: $constraints")
        promise.fail(constraints.toString())
        return promise.future()
      }
      val snapshotValue: AtomicReference<Snapshot<E>> = AtomicReference()
      val uowValue: AtomicReference<UnitOfWork> = AtomicReference()
      val uowIdValue: AtomicReference<Long> = AtomicReference()
      snapshotRepo.retrieve(metadata.entityId)
        .compose { snapshot ->
          if (log.isDebugEnabled) log.debug("got snapshot $snapshot")
          val cachedSnapshot = snapshot ?: Snapshot(commandAware.initialState, 0)
          snapshotValue.set(cachedSnapshot)
          commandAware.handleCmd(metadata.entityId, cachedSnapshot.state, command)
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
          snapshotRepo.upsert(metadata.entityId, newSnapshot)
        }
        .onSuccess {
          val pair: Pair<UnitOfWork, Long> = Pair(uowValue.get(), uowIdValue.get())
          if (log.isDebugEnabled) log.debug("command handling success: $pair")
          promise.complete(pair)
        }.onFailure { err -> promise.fail(err) }

      return promise.future()
    }
  }
}
