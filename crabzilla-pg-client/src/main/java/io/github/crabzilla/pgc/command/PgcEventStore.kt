package io.github.crabzilla.pgc.command

import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.COMMAND_SERIALIZER
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.CommandMetadata
import io.github.crabzilla.core.DOMAIN_EVENT_SERIALIZER
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.EventStore
import io.github.crabzilla.core.OptimisticConcurrencyConflict
import io.github.crabzilla.core.StatefulSession
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class PgcEventStore<A : AggregateRoot, C : Command, E : DomainEvent>(
  val channel: String,
  private val writeModelDb: PgPool,
  private val json: Json
) : EventStore<A, C, E> {

  companion object {
    private val log = LoggerFactory.getLogger(PgcEventStore::class.java)
    const val SQL_SELECT_CURRENT_VERSION =
      """ select max(version) as last_version
            from events
           where ar_id = $1 and ar_name = $2 """
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

    fun checkVersion(conn: SqlConnection): Future<Void> {
      val promise0 = Promise.promise<Void>()
      val params0 = Tuple.of(metadata.aggregateRootId, session.currentState::class.simpleName)
      conn.preparedQuery(SQL_SELECT_CURRENT_VERSION)
        .execute(params0)
        .onFailure { err -> promise0.fail(err) }
        .onSuccess { event1 ->
          val currentVersion = event1.first()?.getInteger("last_version") ?: 0
          when {
            currentVersion == expectedVersionAfterAppend -> {
              val message = "The current version is already the expected new version $expectedVersionAfterAppend"
              promise0.fail(OptimisticConcurrencyConflict(message))
            }
            currentVersion != expectedVersionAfterAppend - 1 -> {
              val message = "The current version [$currentVersion] should be [${expectedVersionAfterAppend - 1}]"
              promise0.fail(OptimisticConcurrencyConflict(message))
            }
            else -> {
              log.info("Version ok")
              promise0.complete()
            }
          }
        }
      return promise0.future()
    }

    fun appendCommand(conn: SqlConnection): Future<Long> { // TODO receber expectedVersion e currentVersion
      val promise0 = Promise.promise<Long>()
      val cmdAsJsonObject: String = json.encodeToString(COMMAND_SERIALIZER, command)
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
            promise0.fail("cannot get cmd_id")
          } else {
            promise0.complete(rowSet.first().getLong("cmd_id"))
            log.info("Append command ok")
          }
        }
      return promise0.future()
    }

    fun appendEvents(conn: SqlConnection, commandId: Long): Future<Void> {
      fun appendEvent(conn: SqlConnection, event: E): Future<Void> {
        val promise0 = Promise.promise<Void>()
        val json = json.encodeToString(DOMAIN_EVENT_SERIALIZER, event)
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
            log.info("Append event ok $event")
            promise0.complete()
          }
        return promise0.future()
      }

      val promise0 = Promise.promise<Void>()
      // TODO fix to support more than 6 events per command
      val futures: List<Future<Void>> = session.appliedEvents().map { event -> appendEvent(conn, event) }
      CompositeFuture.all(futures).onComplete { ar ->
        if (ar.succeeded()) {
          promise0.complete()
          log.info("Append events ok")
        } else {
          promise0.fail(ar.cause())
          log.error("Transaction failed 3: " + ar.cause().message)
        }
      }
      return promise0.future()
    }

    fun notifyPgChannel(conn: SqlConnection): Future<RowSet<Row>>? {
      return conn.query("NOTIFY $channel").execute()
    }

    val promise = Promise.promise<Void>()
    writeModelDb.connection
      .onSuccess { conn ->
        conn.begin()
          .compose { tx ->
            checkVersion(conn)
              .compose { appendCommand(conn) }
              .compose { commandId -> appendEvents(conn, commandId) }
              .compose { notifyPgChannel(conn) }
              .onSuccess {
                tx.commit()
                promise.complete()
              }
              .onFailure { err ->
                tx.rollback()
                promise.fail(err)
              }
              .eventually { conn.close() }
          }
      }
    return promise.future()
  }
}
