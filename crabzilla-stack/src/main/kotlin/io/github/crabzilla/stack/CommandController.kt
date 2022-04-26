package io.github.crabzilla.stack

import io.github.crabzilla.core.CommandComponent
import io.github.crabzilla.core.CommandHandler
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class CommandController<S : Any, C : Any, E : Any>(
  private val vertx: Vertx,
  private val pgPool: PgPool,
  private val commandComponent: CommandComponent<S, C, E>,
  private val repository: CommandRepository<S, C, E>,
  private val options: CommandControllerOptions = CommandControllerOptions()
) {

  companion object {
    private val log = LoggerFactory.getLogger(CommandController::class.java)
    const val SQL_LOCK =
      """ SELECT pg_try_advisory_xact_lock($1, $2) as locked
      """
  }

  private val stateTypeName = commandComponent.stateClassName()
  private val notificationsByStateType = HashSet<String>()
  private val commandHandler: CommandHandler<S, C, E> = commandComponent.commandHandlerFactory.invoke()

  fun startPgNotification(): CommandController<S, C, E> {
    log.info("Starting notifying Postgres for $stateTypeName each ${options.pgNotificationInterval} ms")
    notificationsByStateType.add(stateTypeName)
    vertx.setPeriodic(options.pgNotificationInterval) {
      flushPendingPgNotifications()
    }
    return this
  }

  fun flushPendingPgNotifications(): Future<Void> {
    val initialFuture = Future.succeededFuture<Void>()
    return notificationsByStateType.fold(
      initialFuture
    ) { currentFuture: Future<Void>, stateType: String ->
      currentFuture.compose {
        val query = "NOTIFY ${CrabzillaConstants.POSTGRES_NOTIFICATION_CHANNEL}, '$stateType'"
        pgPool.preparedQuery(query).execute()
          .onSuccess { log.info("Notified postgres: $query") }
          .mapEmpty()
      }
    }.onFailure {
      log.error("Notification to postgres failed {$stateTypeName}")
    }.onSuccess {
      notificationsByStateType.clear()
    }
  }

  fun handle(metadata: CommandMetadata, command: C): Future<CommandSideEffect> {
    return pgPool.withTransaction { conn: SqlConnection ->
      handle(conn, metadata, command)
    }
  }

  fun compose(f: (SqlConnection) -> Future<CommandSideEffect>): Future<CommandSideEffect> {
    return pgPool.withTransaction(f)
  }

  fun handle(conn: SqlConnection, metadata: CommandMetadata, command: C): Future<CommandSideEffect> {
    fun validate(command: C): Future<Void> {
      if (commandComponent.commandValidator != null) {
        val errors = commandComponent.commandValidator!!.validate(command)
        if (errors.isNotEmpty()) {
          return Future.failedFuture(CommandException.ValidationException(errors))
        }
      }
      return Future.succeededFuture()
    }
    fun lock(conn: SqlConnection, lockId: Int, metadata: CommandMetadata): Future<Void> {
      return conn
        .preparedQuery(SQL_LOCK)
        .execute(Tuple.of(stateTypeName.hashCode(), lockId))
        .compose { pgRow ->
          if (pgRow.first().getBoolean("locked")) {
            Future.succeededFuture()
          } else {
            Future.failedFuture(CommandException.LockingException("Can't be locked ${metadata.stateId}"))
          }
        }
    }
    fun getSnapshot(conn: SqlConnection, id: UUID): Future<Snapshot<S>?> {
      return repository.getSnapshot(conn, id)
    }
    fun appendCommand(conn: SqlConnection, command: C, metadata: CommandMetadata): Future<Void> {
      return repository.appendCommand(conn, command, metadata)
    }
    fun appendEvents(conn: SqlConnection, initialVersion: Int, events: List<E>, metadata: CommandMetadata)
            : Future<CommandSideEffect> {
      return repository.appendEvents(conn, initialVersion, events, metadata)
    }
    fun projectEvents(conn: SqlConnection, appendedEvents: List<EventRecord>, projector: EventProjector)
            : Future<Void> {
      log.debug("Will project {} events", appendedEvents.size)
      val initialFuture = Future.succeededFuture<Void>()
      return appendedEvents.fold(
        initialFuture
      ) { currentFuture: Future<Void>, appendedEvent: EventRecord ->
        currentFuture.compose {
          projector.project(conn, appendedEvent)
        }
      }.mapEmpty()
    }

    return validate(command)
      .compose {
        log.debug("Command validated")
        lock(conn, metadata.stateId.hashCode(), metadata)
          .compose {
            log.debug("State locked")
            getSnapshot(conn, metadata.stateId)
          }.compose { snapshot ->
            log.debug("Got snapshot {}", snapshot)
            Future.succeededFuture(Pair(snapshot, commandHandler.handleCommand(command, snapshot?.state)))
          }.compose { pair ->
            val (_, session) = pair
            log.debug("Command handled {}", session.toSessionData())
            appendCommand(conn, command, metadata)
              .map { pair }
          }.compose { pair ->
            val (snapshot, session) = pair
            log.debug("Command appended")
            val originalVersion = snapshot?.version ?: 0
            appendEvents(conn, originalVersion, session.appliedEvents(), metadata)
              .map { Triple(snapshot, session, it) }
          }.compose { triple ->
            val (_, _, commandSideEffect) = triple
            log.debug("Events appended {}", commandSideEffect.toString())
            if (options.eventProjector != null) {
              projectEvents(conn, commandSideEffect.appendedEvents, options.eventProjector!!)
                .onSuccess {
                  log.debug("Events projected")
                }.map { commandSideEffect }
            } else {
              log.debug("EventProjector is null, skipping projecting events")
              Future.succeededFuture(commandSideEffect)
            }
          }.onSuccess {
            if (options.eventBusTopic != null) {
              val message = JsonObject()
                .put("stateType", stateTypeName)
                .put("commandMetadata", metadata.toJsonObject())
                .put("events", it.toJsonArray())
                .put("timestamp", Instant.now())
              vertx.eventBus().publish(options.eventBusTopic, message)
              log.debug("Published to ${message.encodePrettily()} to topic ${options.eventBusTopic}")
            }
            notificationsByStateType.add(stateTypeName)
          }
      }
  }
}
