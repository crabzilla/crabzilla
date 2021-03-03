package io.github.crabzilla.pgc.command

import io.github.crabzilla.core.COMMAND_SERIALIZER
import io.github.crabzilla.core.DOMAIN_EVENT_SERIALIZER
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.UnitOfWork
import io.github.crabzilla.core.UnitOfWorkJournal
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class PgcUowJournal(private val writeModelDb: PgPool, private val json: Json) : UnitOfWorkJournal {

  companion object {
    private val log = LoggerFactory.getLogger(PgcUowJournal::class.java)
    const val SQL_SELECT_CURRENT_VERSION =
      """ select max(version) as last_version from crabz_events where ar_id = $1 and ar_name = $2 """
    const val SQL_APPEND_CMD = """ insert into crabz_commands (cmd_id, cmd_payload) values ($1, $2)"""
    const val SQL_APPEND_EVENT =
      """ insert into crabz_events (event_payload, ar_name, ar_id, version, cmd_id) values
         ($1, $2, $3, $4, $5)"""
  }

  override fun append(uow: UnitOfWork): Future<Void> {

    fun checkVersion(conn: SqlConnection): Future<Void> {
      val promise0 = Promise.promise<Void>()
      val params0 = Tuple.of(uow.aggregateRootId, uow.entityName)
      conn.preparedQuery(SQL_SELECT_CURRENT_VERSION)
        .execute(params0)
          .onFailure { err -> promise0.fail(err) }
          .onSuccess { event1 ->
            val currentVersion = event1.first()?.getInteger("last_version") ?: 0
            if (currentVersion != uow.version - 1) {
              val error = "expected version is ${uow.version - 1} but current version is $currentVersion"
              promise0.fail(error)
            } else {
              log.info("Version ok")
              promise0.complete()
            }
          }
      return promise0.future()
    }

    fun appendCommand(conn: SqlConnection): Future<Void> {
      val promise0 = Promise.promise<Void>()
      val cmdAsJsonObject: String = json.encodeToString(COMMAND_SERIALIZER, uow.command)
      val params = Tuple.of(uow.commandId, JsonObject(cmdAsJsonObject))
      conn.preparedQuery(SQL_APPEND_CMD)
        .execute(params)
        .onFailure { err -> promise0.fail(err) }
        .onSuccess {
            log.info("Append command ok")
            promise0.complete()
        }
      return promise0.future()
    }

    fun appendEvents(conn: SqlConnection): Future<Void> {
        fun appendEvent(conn: SqlConnection, event: DomainEvent): Future<Void> {
            val promise0 = Promise.promise<Void>()
            val eventJson: String = json.encodeToString(DOMAIN_EVENT_SERIALIZER, event)
            val params = Tuple.of(JsonObject(eventJson), uow.entityName, uow.aggregateRootId, uow.version, uow.commandId)
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
        val futures: List<Future<Void>> = uow.events.map { event -> appendEvent(conn, event) }
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
            .compose { appendEvents(conn) }
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
