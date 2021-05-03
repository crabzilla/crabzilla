package io.github.crabzilla.pgc

import io.github.crabzilla.core.AGGREGATE_ROOT_SERIALIZER
import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.AggregateRootConfig
import io.github.crabzilla.core.COMMAND_SERIALIZER
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DOMAIN_EVENT_SERIALIZER
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.StatefulSession
import io.github.crabzilla.pgc.PgcClient.close
import io.github.crabzilla.pgc.PgcClient.commit
import io.github.crabzilla.pgc.PgcClient.rollback
import io.github.crabzilla.stack.CommandException
import io.github.crabzilla.stack.CommandMetadata
import io.github.crabzilla.stack.EventStore
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
import java.util.function.BiFunction

class PgcEventStore<A : AggregateRoot, C : Command, E : DomainEvent>(
  private val config: AggregateRootConfig<A, C, E>,
  private val writeModelDb: PgPool
) : EventStore<A, C, E> {

  /**
   TODO after committing events, it could publish using eventbus.
   Just for observability, not for transactional consumers
   */

  companion object {
    private val log = LoggerFactory.getLogger(PgcEventStore::class.java)
    const val SQL_APPEND_CMD =
      """ insert into commands (cmd_id, causation_id, correlation_id, cmd_payload)
          values ($1, $2, $3, $4)"""
    const val SQL_APPEND_EVENT =
      """ insert into events (event_payload, ar_name, ar_id, version, cmd_id)
        values ($1, $2, $3, $4, $5)"""
  }

  override fun append(command: C, metadata: CommandMetadata, session: StatefulSession<A, E>): Future<Void> {

    val expectedVersionAfterAppend = session.originalVersion + 1

    fun getCurrentVersion(conn: SqlConnection): Future<Int?> {
      fun selectSnapshot(): String {
        return "SELECT version FROM ${config.snapshotTableName.value} WHERE ar_id = $1 FOR SHARE"
      }
      fun extractVersion(rowSet: RowSet<Row>): Int {
        return if (rowSet.size() == 0) {
          0
        } else {
          rowSet.first().getInteger("version")
        }
      }
      val promise0 = Promise.promise<Int?>()
      conn.preparedQuery(selectSnapshot())
        .execute(Tuple.of(metadata.aggregateRootId.id))
        .onSuccess { pgRow ->
          val currentVersion = extractVersion(pgRow)
          log.debug("Got current version: {}", currentVersion)
          when {
            currentVersion == expectedVersionAfterAppend -> {
              val message = "The current version is already the expected new version $expectedVersionAfterAppend"
              log.debug(message)
              promise0.fail(CommandException.WriteConcurrencyException(message))
            }
            currentVersion != expectedVersionAfterAppend - 1 -> {
              val message = "The current version [$currentVersion] should be [${expectedVersionAfterAppend - 1}]"
              log.debug(message)
              promise0.fail(CommandException.WriteConcurrencyException(message))
            }
            else -> {
              log.debug("Version is {}", currentVersion)
              promise0.complete(currentVersion)
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
          expectedVersionAfterAppend,
          JsonObject(newSTateAsJson),
          metadata.aggregateRootId.id
        )
        conn.preparedQuery(insert())
          .execute(params)
          .onFailure { err ->
            log.error(err.message, err)
            promise0.fail(err)
          }
          .onSuccess {
            promise0.complete()
            log.debug("Successfully inserted version")
          }
      } else {
        val params = Tuple.of(
          expectedVersionAfterAppend,
          JsonObject(newSTateAsJson),
          metadata.aggregateRootId.id,
          expectedVersionAfterAppend - 1
        )
        conn.preparedQuery(update())
          .execute(params)
          .onFailure { err ->
            log.error(err.message, err)
            promise0.fail(err)
          }
          .onSuccess {
            promise0.complete()
            log.debug("Successfully updated version")
          }
      }

      return promise0.future()
    }

    fun appendCommand(conn: SqlConnection): Future<Void> {
      val promise0 = Promise.promise<Void>()
      val cmdAsJsonObject: String = config.json.encodeToString(COMMAND_SERIALIZER, command)
      val params = Tuple.of(
        metadata.commandId.id,
        metadata.causationId.id,
        metadata.correlationId.id,
        JsonObject(cmdAsJsonObject)
      )
      conn.preparedQuery(SQL_APPEND_CMD)
        .execute(params)
        .onFailure { err -> promise0.fail(err) }
        .onSuccess {
          promise0.complete()
          log.debug("Append command ok")
        }
      return promise0.future()
    }

    fun appendEvents(conn: SqlConnection): Future<Void> {
      fun appendEvent(event: E): Future<Boolean> {
        val promise0 = Promise.promise<Boolean>()
        val json = config.json.encodeToString(DOMAIN_EVENT_SERIALIZER, event)
        val params = Tuple.of(
          JsonObject(json),
          session.currentState::class.simpleName,
          metadata.aggregateRootId.id,
          expectedVersionAfterAppend,
          metadata.commandId.id
        )
        conn.preparedQuery(SQL_APPEND_EVENT)
          .execute(params)
          .onFailure { promise0.fail(it) }
          .onSuccess {
            log.debug("Append event ok {}", event)
            promise0.complete(true)
          }
        return promise0.future()
      }
      fun <A, B> foldLeft(iterator: Iterator<A>, identity: B, bf: BiFunction<B, A, B>): B {
        var result = identity
        while (iterator.hasNext()) {
          val next = iterator.next()
          result = bf.apply(result, next)
        }
        return result
      }

      val promise0 = Promise.promise<Void>()
      val initialFuture = Future.succeededFuture<Boolean>(true)

      foldLeft(
        session.appliedEvents().iterator(), initialFuture,
        { currentFuture: Future<Boolean>,
          event: E ->
          currentFuture.compose { successful ->
            if (successful) {
              appendEvent(event)
            } else {
              Future.failedFuture("The latest successful event was $event")
            }
          }
        }
      ).onComplete {
        if (it.failed()) {
          promise0.fail(it.cause())
        } else {
          promise0.complete()
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
            promise.fail(it)
            close(conn)
          }
          .onSuccess { tx: Transaction ->
            getCurrentVersion(conn)
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
                      .compose { appendEvents(conn) }
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
                            log.debug("Events successfully committed")
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
