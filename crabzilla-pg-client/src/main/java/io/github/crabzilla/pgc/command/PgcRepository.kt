package io.github.crabzilla.pgc.command

import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.COMMAND_SERIALIZER
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.EventDeserializer
import io.github.crabzilla.core.EventSerializer
import io.github.crabzilla.core.Failure
import io.github.crabzilla.core.Repository
import io.github.crabzilla.core.Success
import io.github.crabzilla.core.Try
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
import java.util.*

class PgcRepository<E: DomainEvent, A: AggregateRoot>(
  private val writeModelDb: PgPool,
  private val json: Json,
  private val ser: EventSerializer<E>,
  private val des: EventDeserializer<E>) // TODO usar um vertx schema validator antes de salvar
  : Repository<A> {

  companion object {
    private val log = LoggerFactory.getLogger(PgcRepository::class.java)
    const val SQL_SELECT_CURRENT_VERSION =
      """ select max(version) as last_version from crabz_events where ar_id = $1 and ar_name = $2 """
    const val SQL_APPEND_CMD = """ insert into crabz_commands (external_cmd_id, cmd_payload) values ($1, $2) returning cmd_id"""
    const val SQL_APPEND_EVENT =
      """ insert into crabz_events (event_payload, ar_name, ar_id, version, cmd_id) values
         ($1, $2, $3, $4, $5)"""
  }

  override fun append(aggregate: A, expectedVersion: Int, command: Command, externalCommandId: UUID): Future<Void> {

    fun checkVersion(conn: SqlConnection): Future<Void> {
      val promise0 = Promise.promise<Void>()
      val params0 = Tuple.of(aggregate.id(), AggregateRoot::class.simpleName)
      conn.preparedQuery(SQL_SELECT_CURRENT_VERSION)
        .execute(params0)
          .onFailure { err -> promise0.fail(err) }
          .onSuccess { event1 ->
            val currentVersion = event1.first()?.getInteger("last_version") ?: 0
            if (currentVersion != expectedVersion -1) {
              val error = "expected version is {$expectedVersion -1} but current version is $currentVersion"
              promise0.fail(error)
            } else {
              log.info("Version ok")
              promise0.complete()
            }
          }
      return promise0.future()
    }

    fun appendCommand(conn: SqlConnection): Future<Long> {
      val promise0 = Promise.promise<Long>()
      val cmdAsJsonObject: String = json.encodeToString(COMMAND_SERIALIZER, command)
      val params = Tuple.of(externalCommandId, JsonObject(cmdAsJsonObject))
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
          when(val eventJson: Try<JsonObject> = ser.toJson(event)) {
            is Success -> {
              val params = Tuple.of(
                eventJson.get(),
                aggregate::class.simpleName,
                aggregate.id(),
                expectedVersion,
                commandId)
              conn.preparedQuery(SQL_APPEND_EVENT)
                .execute(params)
                .onFailure { err -> promise0.fail(err) }
                .onSuccess {
                  log.info("Append event ok $event")
                  promise0.complete()
                }
            }
            is Failure -> {
              promise0.fail(eventJson.e)
            }
          }
          return promise0.future()
        }
        val promise0 = Promise.promise<Void>()
        // TODO fix to support more than 6 events per command
        // TODO fix aggregate root must know if events are pending
        val futures: List<Future<Void>> = aggregate.changes.map { event -> appendEvent(conn, event as E) }
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

    val promise = Promise.promise<Void>()
    writeModelDb.connection
      .onSuccess { conn ->
        conn.begin()
        .compose { tx ->
          checkVersion(conn)
            .compose { appendCommand(conn) }
            .compose { commandId -> appendEvents(conn, commandId) }
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
