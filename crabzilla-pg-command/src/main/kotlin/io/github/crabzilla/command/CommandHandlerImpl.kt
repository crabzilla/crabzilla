package io.github.crabzilla.command

import io.github.crabzilla.command.internal.GivenAllEventsViewEffectHandler
import io.github.crabzilla.command.internal.GivenEachEventViewEffectHandler
import io.github.crabzilla.command.internal.ViewEffectHandler
import io.github.crabzilla.context.CrabzillaContext
import io.github.crabzilla.context.PgNotifierVerticle
import io.github.crabzilla.core.Session
import io.github.crabzilla.stream.StreamMustBeNewException
import io.github.crabzilla.stream.StreamRepository.Companion.NO_STREAM
import io.github.crabzilla.stream.StreamRepositoryImpl
import io.github.crabzilla.stream.StreamSnapshot
import io.github.crabzilla.stream.StreamWriterImpl
import io.github.crabzilla.stream.TargetStream
import io.vertx.core.Future
import io.vertx.core.Future.failedFuture
import io.vertx.core.Future.succeededFuture
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory
import java.util.*

class CommandHandlerImpl<S : Any, C : Any, E : Any>(
  private val context: CrabzillaContext,
  private val config: CommandHandlerConfig<S, C, E>,
) : CommandHandler<S, C, E> {
  override fun handle(
    targetStream: TargetStream,
    command: C,
    commandMetadata: CommandMetadata,
  ): Future<CommandHandlerResult<S, E>> {
    return withinTransaction { conn: SqlConnection ->
      handleWithinTransaction(conn, targetStream, command, commandMetadata)
    }
  }

  override fun handleWithinTransaction(
    sqlConnection: SqlConnection,
    targetStream: TargetStream,
    command: C,
    commandMetadata: CommandMetadata,
  ): Future<CommandHandlerResult<S, E>> {
    if (logger.isDebugEnabled) logger.debug("Will handle a new command for stream {}", targetStream.name)

    fun appendCommand(
      causationId: UUID?,
      correlationId: UUID?,
      cmdAsJson: JsonObject?,
      streamId: Int,
    ): Future<Void> {
      if (logger.isTraceEnabled) logger.trace("Will append command {} as {} metadata {}", command, cmdAsJson, commandMetadata)
      val params =
        Tuple.of(
          commandMetadata.commandId,
          causationId,
          correlationId,
          cmdAsJson,
          streamId,
          commandMetadata.metadata,
        )
      return sqlConnection.preparedQuery(SQL_APPEND_CMD).execute(params).mapEmpty()
    }

    val streamRepositoryImpl =
      StreamRepositoryImpl(
        conn = sqlConnection,
        targetStream = targetStream,
        initialState = config.initialState,
        eventHandler = config.evolveFunction,
        eventSerDer = config.eventSerDer,
      )

    return streamRepositoryImpl.getStreamId()
      .compose { streamId ->
        val params = Tuple.of(targetStream.stateType(), targetStream.stateId(), targetStream.name)
        if (streamId != NO_STREAM && targetStream.mustBeNew) {
          throw StreamMustBeNewException("Stream ${targetStream.name} must be new")
        }
        succeededFuture(streamId)
        if (streamId == NO_STREAM) {
          if (logger.isDebugEnabled) logger.debug("Will create stream {}", targetStream.name)
          sqlConnection.preparedQuery(SQL_INSERT_STREAM)
            .execute(params)
            .map { it.first().getInteger("id") }
        } else {
          succeededFuture(streamId)
        }
      }
      .compose { streamId ->
        val streamWriter =
          StreamWriterImpl<S, E>(
            conn = sqlConnection,
            targetStream = targetStream,
            streamId = streamId,
            uuidFunction = context.uuidFunction,
            eventSerDer = config.eventSerDer,
          )
        streamWriter.lockTargetStream(config.lockingImplementation)
          .compose {
            if (logger.isDebugEnabled) logger.debug("Stream locked {}", streamId)
            val cachedSnapshot = config.snapshotCache?.getIfPresent(streamId)
            if (logger.isDebugEnabled) logger.debug("Got cached snapshot version {}", cachedSnapshot?.version)
            streamRepositoryImpl.getSnapshot(streamId, cachedSnapshot)
              .compose { snapshot ->
                if (logger.isDebugEnabled) logger.debug("Got latest snapshot version {}", snapshot.version)
                try {
                  if (logger.isTraceEnabled) logger.trace("Will handle command {} on state {}", command, snapshot.state)
                  val session =
                    Session(
                      snapshot.state,
                      decideFunction = config.decideFunction,
                      evolveFunction = config.evolveFunction,
                      injectFunction = config.injectFunction,
                    )
                  session.decide(command)
                  succeededFuture(Pair(snapshot, session))
                } catch (e: Exception) {
                  val error = BusinessException(e.message ?: "Unknown", e)
                  failedFuture(error)
                }
              }
              .compose { pair ->
                val (streamSnapshot: StreamSnapshot<S>, session: Session<C, S, E>) = pair
                if (logger.isDebugEnabled) logger.debug("Command handled")
                streamWriter.appendEvents(
                  streamSnapshot = streamSnapshot,
                  events = session.appliedEvents(),
                )
                  .map { eventsAppended -> Triple(streamSnapshot, session, eventsAppended) }
              }
          }
      }
      .compose { triple ->
        val (snapshot, session, appendedEvents) = triple

        val lastEvent = appendedEvents.last()
        val newSnapshot =
          StreamSnapshot(
            streamId = snapshot.streamId,
            state = session.currentState(),
            version = lastEvent.metadata.version,
            causationId = lastEvent.metadata.causationId,
            correlationId = lastEvent.metadata.correlationId,
          )
        val result = CommandHandlerResult(newSnapshot, session.appliedEvents(), appendedEvents.map { it.metadata })
        if (logger.isDebugEnabled) logger.debug("{} events appended", appendedEvents.size)
        if (config.viewEffect != null) {
          val viewEffectHandler: ViewEffectHandler<S, E> =
            when (config.viewEffect) {
              is ViewEffect.GivenAllEventsViewEffect ->
                GivenAllEventsViewEffectHandler(
                  sqlConnection,
                  config.viewEffect,
                  config.viewTrigger,
                )
              is ViewEffect.GivenEachEventViewEffect ->
                GivenEachEventViewEffectHandler(
                  sqlConnection,
                  config.viewEffect,
                  config.viewTrigger,
                )
            }
          viewEffectHandler
            .handle(result)
            .onSuccess {
              if (logger.isDebugEnabled) logger.debug("Events projected")
            }.map { triple }
        } else {
          if (logger.isDebugEnabled) logger.debug("ViewEffect is null, skipping projecting events")
          succeededFuture(triple)
        }
      }.compose {
        val (snapshot, session, appendedEvents) = it
        if (config.persistCommands != false) {
          val cmdAsJson = config.commandSerDer?.toJson(command)
          appendCommand(
            causationId = snapshot.causationId,
            correlationId = snapshot.correlationId,
            cmdAsJson = cmdAsJson,
            streamId = snapshot.streamId,
          )
        } else {
          succeededFuture()
        }
          .map {
            val lastEvent = appendedEvents.last()
            val newSnapshot =
              StreamSnapshot(
                streamId = snapshot.streamId,
                state = session.currentState(),
                version = lastEvent.metadata.version,
                causationId = lastEvent.metadata.causationId,
                correlationId = lastEvent.metadata.correlationId,
              )
            CommandHandlerResult(newSnapshot, session.appliedEvents(), appendedEvents.map { it.metadata })
          }
      }
      .onSuccess {
        if (config.snapshotCache != null) {
          config.snapshotCache.put(it.snapshot.streamId, it.snapshot)
        }
        if (config.notifyPostgres) {
          context.vertx.eventBus()
            .publish(PgNotifierVerticle.PG_NOTIFIER_ADD_ENDPOINT, targetStream.stateType())
        }
        if (logger.isDebugEnabled) logger.debug("Transaction committed")
      }.onFailure {
        logger.error("Transaction aborted {}", it.message)
      }
  }

  override fun withinTransaction(
    commandOperation: (SqlConnection) -> Future<CommandHandlerResult<S, E>>,
  ): Future<CommandHandlerResult<S, E>> {
    return context.pgPool.withTransaction(commandOperation)
  }

  companion object {
    private val logger = LoggerFactory.getLogger(CommandHandlerImpl::class.java)
    private const val SQL_INSERT_STREAM = """
      INSERT
        INTO streams (state_type, state_id, name)
      VALUES ($1, $2, $3) RETURNING id
    """
    private const val SQL_APPEND_CMD =
      """ INSERT INTO commands (command_id, causation_id, correlation_id, command_payload, stream_id, command_metadata)
          VALUES ($1, $2, $3, $4, $5, $6)"""
  }
}
