// package io.github.crabzilla
//
// import io.github.crabzilla.core.AggregateRoot
// import io.github.crabzilla.core.Command
// import io.github.crabzilla.core.CommandHandler
// import io.github.crabzilla.core.CommandMetadata
// import io.github.crabzilla.core.DomainEvent
// import io.github.crabzilla.core.EventHandler
// import io.github.crabzilla.core.EventStore
// import io.github.crabzilla.core.Snapshot
// import io.github.crabzilla.core.SnapshotRepository
// import io.github.crabzilla.core.StatefulSession
// import io.vertx.core.Future
// import io.vertx.core.Promise
// import org.slf4j.LoggerFactory
//
// class CommandController<A : AggregateRoot, C: Command, E: DomainEvent>(
//  private val handler: CommandHandler<A, C, E>,
//  private val snapshotRepo: SnapshotRepository<A, C, E>,
//  private val eventStore: EventStore<A, C, E>
// ) {
//  companion object {
//    internal val log = LoggerFactory.getLogger(CommandController::class.java)
//  }
//  fun handle(metadata: CommandMetadata, command: C): Future<Void> { // TODO return snapshot, events and metadata
//    val promise = Promise.promise<Void>()
//    if (log.isDebugEnabled) log.debug("received $metadata\n $command")
//    snapshotRepo.get(metadata.aggregateRootId)
//      .compose { snapshot ->
//        handler.handleCommand(command, snapshot)
//          .fold { result -> 1 }}
//      }
//
//        .onFailure { promise.fail(it.cause) }
//
//
//
//
//
//
//
//      .compose { snapshot: Snapshot<A>? ->
//        if (log.isDebugEnabled) log.debug("got snapshot $snapshot")
//        val cachedSnapshot = snapshot ?: Snapshot(commandAware.initialState, 0)
//        snapshotValue.set(cachedSnapshot)
//        commandAware.handleCmd(metadata.aggregateRootId, cachedSnapshot.state, command)
//      }
//      .compose { eventsList ->
//        val request = Triple(metadata, command, snapshotValue.get())
//        val uow = toUnitOfWork(request, eventsList)
//        if (log.isDebugEnabled) log.debug("got unitOfWork $uow")
//        // append to journal
//        uowValue.set(uow)
//        eventStore.append(uow)
//      }
//      .onSuccess {
//        if (log.isDebugEnabled) log.debug("command handling success: $pair")
//        promise.complete()
//      }.onFailure { err -> promise.fail(err) }
//
//    return promise.future()
//  }
// }
