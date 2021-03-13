package io.github.crabzilla
//
// import io.vertx.core.Future
// import io.vertx.core.Promise
// import java.util.UUID
// import java.util.concurrent.atomic.AtomicReference
// import org.slf4j.LoggerFactory
//
// data class CommandMetadata(
//  val aggregateRootId: Int,
//  val entityName: String,
//  val commandName: String,
//  val commandId: UUID = UUID.randomUUID()
// )
//
// typealias CommandContext<A> = Triple<CommandMetadata, Command, Snapshot<A>>
//
// class CommandController<A : AggregateRoot>(
//  private val commandAware: AggregateRootCommandAware<A, >,
//  private val snapshotRepo: SnapshotRepository<A>,
//  private val uowJournal: UnitOfWorkJournal
// ) {
//  companion object {
//    internal val log = LoggerFactory.getLogger(CommandController::class.java)
//  }
//  fun handle(metadata: CommandMetadata, command: Command): Future<Pair<UnitOfWork, Long>> {
//    fun toUnitOfWork(ctx: CommandContext<A>, events: List<DomainEvent>): UnitOfWork {
//      val (cmdMetadata, _, snapshot) = ctx
//      val (entityId, entityName, _, commandId) = cmdMetadata
//      return UnitOfWork(entityName, entityId, commandId, command, snapshot.version + 1, events)
//    }
//    val promise = Promise.promise<Pair<UnitOfWork, Long>>()
//    if (log.isDebugEnabled) log.debug("received $metadata\n $command")
//    val constraints = commandAware.validateCmd(command)
//    if (constraints.isNotEmpty()) {
//      log.error("Command is invalid: $constraints")
//      promise.fail(constraints.toString())
//      return promise.future()
//    }
//    val snapshotValue: AtomicReference<Snapshot<A>> = AtomicReference()
//    val uowValue: AtomicReference<UnitOfWork> = AtomicReference()
//    val uowIdValue: AtomicReference<Long> = AtomicReference()
//    snapshotRepo.retrieve(metadata.aggregateRootId)
//      .compose { snapshot ->
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
//        uowJournal.append(uow)
//      }
//      .compose {
//        // compute new snapshot
//        if (log.isDebugEnabled) log.debug("computing new snapshot")
//        uowValue.get().events.forEach { e ->
//          snapshotValue.get().state.apply(e)
//        }
//        val newSnapshot = Snapshot(snapshotValue.get().state, uowValue.get().version)
//        if (log.isDebugEnabled) log.debug("now will store snapshot $newSnapshot")
//        snapshotRepo.upsert(metadata.aggregateRootId, newSnapshot)
//      }
//      .onSuccess {
//        val pair: Pair<UnitOfWork, Long> = Pair(uowValue.get(), uowIdValue.get())
//        if (log.isDebugEnabled) log.debug("command handling success: $pair")
//        promise.complete(pair)
//      }.onFailure { err -> promise.fail(err) }
//
//    return promise.future()
//  }
// }
