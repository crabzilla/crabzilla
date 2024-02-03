package io.github.crabzilla.writer

import io.github.crabzilla.context.CrabzillaContext
import io.github.crabzilla.context.CrabzillaContext.Companion.POSTGRES_NOTIFICATION_CHANNEL
import io.github.crabzilla.context.EventMetadata
import io.github.crabzilla.context.EventsProjector
import io.github.crabzilla.context.TargetStream
import io.github.crabzilla.core.CrabzillaCommandsSession
import io.github.crabzilla.stream.StreamMustBeNewException
import io.github.crabzilla.stream.StreamRepository.Companion.NO_STREAM
import io.github.crabzilla.stream.StreamRepositoryImpl
import io.github.crabzilla.stream.StreamSnapshot
import io.github.crabzilla.stream.StreamWriterImpl
import io.vertx.core.Future
import io.vertx.core.Future.failedFuture
import io.vertx.core.Future.succeededFuture
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory
import java.util.*

class WriterApiImpl<S : Any, C : Any, E : Any>(
  private val context: CrabzillaContext,
  private val config: WriterConfig<S, C, E>,
) : WriterApi<C> {
  private val log = LoggerFactory.getLogger(WriterApiImpl::class.java)

  override fun handle(
    targetStream: TargetStream,
    command: C,
    commandMetadata: CommandMetadata,
  ): Future<EventMetadata> {
    return context.withinTransaction { conn: SqlConnection ->
      handleWithinTransaction(conn, targetStream, command, commandMetadata)
    }
  }

  override fun handleWithinTransaction(
    sqlConnection: SqlConnection,
    targetStream: TargetStream,
    command: C,
    commandMetadata: CommandMetadata,
  ): Future<EventMetadata> {
    fun appendCommand(
      causationId: UUID?,
      correlationId: UUID?,
      cmdAsJson: JsonObject?,
      streamId: Int,
    ): Future<Void> {
      log.debug("Will append command {} as {} metadata {}", command, cmdAsJson, commandMetadata)
      val params =
        Tuple.of(
          commandMetadata.commandId ?: context.uuidFunction.invoke(),
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
        eventHandler = config.eventHandler,
        eventSerDer = config.eventSerDer,
      )

    return streamRepositoryImpl.getStreamId()
      .compose { streamId ->
        val params = Tuple.of(targetStream.stateType(), targetStream.stateId(), targetStream.name)
        if (streamId != NO_STREAM && targetStream.mustBeNew) {
          throw StreamMustBeNewException("Stream ${targetStream.name} must be new")
        }
        if (streamId == NO_STREAM) {
          log.debug("Will create stream {}", targetStream.name)
          sqlConnection.preparedQuery(StreamWriterImpl.SQL_INSERT_STREAM)
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
        streamWriter.lockTargetStream()
          .compose {
            log.debug("Stream locked {}", streamId)
            streamRepositoryImpl.getSnapshot(streamId)
              .compose { snapshot ->
                log.debug("Got snapshot {}", snapshot)
                try {
                  log.debug("Will handle command {} on state {}", command, snapshot)
                  val session =
                    CrabzillaCommandsSession(
                      snapshot.state,
                      commandHandler = config.commandHandler,
                      eventHandler = config.eventHandler,
                    )
                  session.handle(command)
                  succeededFuture(Pair(snapshot, session))
                } catch (e: Exception) {
                  val error = BusinessException(e.message ?: "Unknown", e)
                  failedFuture(error)
                }
              }
              .compose { pair ->
                val (streamSnapshot: StreamSnapshot<S>, session: CrabzillaCommandsSession<C, S, E>) = pair
                log.debug("Command handled")
                streamWriter.appendEvents(
                  streamSnapshot = streamSnapshot,
                  events = session.appliedEvents(),
                )
                  .map { eventsAppended -> Triple(streamSnapshot, session, eventsAppended) }
              }
          }
      }
      .compose { triple ->
        val (_, _, appendedEvents) = triple
        log.debug("Events appended {}", appendedEvents)
        if (config.viewEffect != null) {
          EventsProjector(sqlConnection, config.viewEffect, config.viewTrigger)
            .projectEvents(appendedEvents)
            .onSuccess {
              log.debug("Events projected")
            }.map { triple }
        } else {
          log.debug("ViewEffect is null, skipping projecting events")
          succeededFuture(triple)
        }
      }.compose {
        val (snapshot, _, appendedEvents) = it
        val cmdAsJson = config.commandSerDer?.toJson(command)
        appendCommand(
          causationId = snapshot.causationId,
          correlationId = snapshot.correlationId,
          cmdAsJson = cmdAsJson,
          streamId = snapshot.streamId,
        )
          .map { appendedEvents.last().metadata }
      }
      .onSuccess {
        val query = "NOTIFY $POSTGRES_NOTIFICATION_CHANNEL, '${targetStream.stateType()}'"
        sqlConnection.preparedQuery(query).execute()
          .onSuccess { log.debug("Notified postgres: $query") }
        log.debug("Transaction committed")
      }.onFailure {
        log.debug("Transaction aborted {}", it.message)
      }
  }

  companion object {
    private const val SQL_APPEND_CMD =
      """ INSERT INTO commands (command_id, causation_id, correlation_id, command_payload, stream_id, command_metadata)
          VALUES ($1, $2, $3, $4, $5, $6)"""
  }
}
