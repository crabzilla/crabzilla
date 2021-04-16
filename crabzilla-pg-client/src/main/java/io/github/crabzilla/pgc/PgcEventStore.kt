package io.github.crabzilla.pgc

import io.github.crabzilla.core.AGGREGATE_ROOT_SERIALIZER
import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.AggregateRootConfig
import io.github.crabzilla.core.COMMAND_SERIALIZER
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DOMAIN_EVENT_SERIALIZER
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.Snapshot
import io.github.crabzilla.core.StatefulSession
import io.github.crabzilla.pgc.PgcClient.close
import io.github.crabzilla.pgc.PgcClient.commit
import io.github.crabzilla.pgc.PgcClient.rollback
import io.github.crabzilla.stack.CommandMetadata
import io.github.crabzilla.stack.EventStore
import io.github.crabzilla.stack.OptimisticConcurrencyConflict
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Transaction
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory

class PgcEventStore<A : AggregateRoot, C : Command, E : DomainEvent>(
  val topic: String,
  private val writeModelDb: PgPool,
  private val config: AggregateRootConfig<A, C, E>
) : EventStore<A, C, E> {

  /**
   TODO after committing events, it could use topic property to publish to it using eventbus.
   Just for observability, not for transactional consumers
   */

  companion object {
    private val log = LoggerFactory.getLogger(PgcEventStore::class.java)
    const val SQL_APPEND_CMD =
      """ insert into commands (ar_id, external_cmd_id, causation_id, correlation_id, cmd_payload)
          values ($1, $2, $3, $4, $5)
         returning cmd_id"""
    const val SQL_APPEND_EVENT =
      """ insert into events (event_payload, ar_name, ar_id, version, cmd_id)
        values ($1, $2, $3, $4, $5)"""
  }

  override fun append(command: C, metadata: CommandMetadata, session: StatefulSession<A, E>): Future<Void> {

    val expectedVersionAfterAppend = session.originalVersion + 1

    fun getCurrentSnapshot(conn: SqlConnection): Future<Snapshot<A>?> {
      fun selectSnapshot(): String {
        return "SELECT version, json_content FROM ${config.snapshotTableName.value} WHERE ar_id = $1 for share"
      }
      fun snapshot(rowSet: RowSet<Row>): Snapshot<A>? {
        return if (rowSet.size() == 0) {
          null
        } else {
          val stateAsJson: JsonObject = rowSet.first().get(JsonObject::class.java, 1)
          val state = config.json.decodeFromString(AGGREGATE_ROOT_SERIALIZER, stateAsJson.encode()) as A
          Snapshot(state, rowSet.first().getInteger("version"))
        }
      }
      val promise0 = Promise.promise<Snapshot<A>?>()
      conn.preparedQuery(selectSnapshot())
        .execute(Tuple.of(metadata.aggregateRootId))
        .onSuccess { pgRow ->
          val snapshot = snapshot(pgRow)
          val currentVersion = snapshot?.version ?: 0
          if (log.isDebugEnabled) log.debug("Got current version: $currentVersion")
          when {
            currentVersion == expectedVersionAfterAppend -> {
              val message = "The current version is already the expected new version $expectedVersionAfterAppend"
              if (log.isDebugEnabled) log.debug(message)
              promise0.fail(OptimisticConcurrencyConflict(message))
            }
            currentVersion != expectedVersionAfterAppend - 1 -> {
              val message = "The current version [$currentVersion] should be [${expectedVersionAfterAppend - 1}]"
              if (log.isDebugEnabled) log.debug(message)
              promise0.fail(OptimisticConcurrencyConflict(message))
            }
            else -> {
              if (log.isDebugEnabled) log.debug("Snapshot is $snapshot")
              promise0.complete(snapshot)
            }
          }
        }
        .onFailure {
          log.error("When get snapshot", it)
          promise0.fail(it)
        }
      return promise0.future()
    }

    fun updateVersion(conn: SqlConnection): Future<Void> {
      fun insert(): String {
        return "insert into ${config.snapshotTableName.value} (version, json_content, ar_id) " +
          " values ($1, $2, $3)"
      }
      fun update(): String {
        return "update ${config.snapshotTableName.value} set version = $1, json_content = $2 " +
          " where ar_id = $3 and version = $4"
      }
      val promise0 = Promise.promise<Void>()
      val newSTateAsJson: String = config.json.encodeToString(AGGREGATE_ROOT_SERIALIZER, session.currentState)
      if (session.originalVersion == 0) {
        val params = Tuple.of(
          expectedVersionAfterAppend, JsonObject(newSTateAsJson), metadata.aggregateRootId
        )
        conn.preparedQuery(insert())
          .execute(params)
          .onFailure { err ->
            log.error(err.message, err)
            promise0.fail(err)
          }
          .onSuccess {
            promise0.complete()
            if (log.isDebugEnabled) log.debug("Successfully inserted version")
          }
      } else {
        val params = Tuple.of(
          expectedVersionAfterAppend, JsonObject(newSTateAsJson), metadata.aggregateRootId, expectedVersionAfterAppend - 1
        )
        log.info(update())
        log.info(params.deepToString())
        conn.preparedQuery(update())
          .execute(params)
          .onFailure { err ->
            log.error(err.message, err)
            promise0.fail(err)
          }
          .onSuccess {
            promise0.complete()
            if (log.isDebugEnabled) log.debug("Successfully updated version")
          }
      }

      return promise0.future()
    }

    fun appendCommand(conn: SqlConnection): Future<Long> {
      val promise0 = Promise.promise<Long>()
      val cmdAsJsonObject: String = config.json.encodeToString(COMMAND_SERIALIZER, command)
      val params = Tuple.of(
        metadata.aggregateRootId, metadata.id, metadata.causationId, metadata.correlationID,
        JsonObject(cmdAsJsonObject)
      )
      conn.preparedQuery(SQL_APPEND_CMD)
        .execute(params)
        .onFailure { err -> promise0.fail(err) }
        .onSuccess {
          val rowSet: RowSet<Row> = it.value()
          if (rowSet.size() == 0) {
            promise0.fail("Append command error: missing cmd_id")
          } else {
            promise0.complete(rowSet.first().getLong("cmd_id"))
            if (log.isDebugEnabled) log.debug("Append command ok")
          }
        }
      return promise0.future()
    }

    fun appendEvents(conn: SqlConnection, commandId: Long): Future<Void> {
      fun appendEvent(conn: SqlConnection, event: E): Future<Void> {
        val promise0 = Promise.promise<Void>()
        val json = config.json.encodeToString(DOMAIN_EVENT_SERIALIZER, event)
        val params = Tuple.of(
          JsonObject(json),
          session.currentState::class.simpleName,
          metadata.aggregateRootId,
          expectedVersionAfterAppend,
          commandId
        )
        conn.preparedQuery(SQL_APPEND_EVENT)
          .execute(params)
          .onFailure { err -> promise0.fail(err) }
          .onSuccess {
            if (log.isDebugEnabled) log.debug("Append event ok $event")
            promise0.complete()
          }
        return promise0.future()
      }
      val promise0 = Promise.promise<Void>()
      val futures: List<Future<Void>> = session.appliedEvents().map { event -> appendEvent(conn, event) }
      CompositeFuture.all(futures).onComplete { ar -> // TODO support more than 6 events per command
        if (ar.succeeded()) {
          promise0.complete()
          if (log.isDebugEnabled) log.debug("Append events ok")
        } else {
          promise0.fail(ar.cause())
          log.error("Transaction failed", ar.cause())
        }
      }
      return promise0.future()
    }

    val promise = Promise.promise<Void>()
    writeModelDb.connection
      .onFailure {
        promise.fail(it)
      }
      .onSuccess { conn ->
        conn.begin()
          .onFailure {
            log.error(it.message, it)
            promise.fail(it)
            close(conn)
          }
          .onSuccess { tx: Transaction ->
            getCurrentSnapshot(conn)
              .onFailure {
                rollback(tx, it)
                close(conn)
                promise.fail(it)
              }
              .onSuccess {
                updateVersion(conn)
                  .onFailure {
                    rollback(tx, it)
                    close(conn)
                    promise.fail(it)
                  }
                  .onSuccess {
                    appendCommand(conn)
                      .compose { commandId -> appendEvents(conn, commandId) }
                      .onFailure {
                        rollback(tx, it)
                        close(conn)
                        promise.fail(it)
                      }
                      .onSuccess {
                        commit(tx)
                          .onFailure {
                            rollback(tx, it)
                            close(conn)
                            promise.fail(it)
                          }.onSuccess {
                            if (log.isDebugEnabled) log.debug("Events successfully committed to $topic")
                            promise.complete()
                            close(conn)
                          }
                      }
                  }
              }
          }
      }
    return promise.future()
  }
}
