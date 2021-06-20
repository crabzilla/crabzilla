package io.github.crabzilla.pgc

import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.AggregateRootConfig
import io.github.crabzilla.core.Command
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
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Transaction
import io.vertx.sqlclient.Tuple
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BiFunction

class PgcEventStore<A : AggregateRoot, C : Command, E : DomainEvent>(
  private val config: AggregateRootConfig<A, C, E>,
  private val writeModelDb: PgPool,
  private val saveCommandOption: Boolean = true
) : EventStore<A, C, E> {

  companion object {
    private val log = LoggerFactory.getLogger(PgcEventStore::class.java)
    const val SQL_LOCK_VERSION =
      """ SELECT version 
          FROM SNAPSHOTS 
          WHERE ar_id = $1 
            AND ar_type = $2   
          FOR SHARE"""
    const val SQL_APPEND_CMD =
      """ INSERT INTO commands (cmd_id, cmd_payload)
          VALUES ($1, $2)"""
    const val SQL_APPEND_EVENT =
      """ INSERT INTO events (event_payload, ar_name, ar_id, version, correlation_id, causation_id)
          VALUES ($1, $2, $3, $4, $5, $6) returning id"""
    const val SQL_INSERT_VERSION =
      """ INSERT INTO snapshots (version, json_content, ar_id, ar_type)
          VALUES ($1, $2, $3, $4)"""
    const val SQL_UPDATE_VERSION =
      """ UPDATE snapshots 
          SET version = $1, json_content = $2 
          WHERE ar_id = $3 
          AND version = $4"""
  }

  override fun append(command: C, metadata: CommandMetadata, session: StatefulSession<A, E>): Future<Void> {

    fun lockSnapshotIfVersionMatches(conn: SqlConnection): Future<Void> {
      val promise0 = Promise.promise<Void>()
      conn.preparedQuery(SQL_LOCK_VERSION)
        .execute(Tuple.of(metadata.aggregateRootId.id, config.name))
        .onSuccess { rowSet ->
          val currentVersion = if (rowSet.size() == 0) { 0 } else { rowSet.first().getInteger("version") }
          log.debug("Got current version: {}", currentVersion)
          if (currentVersion != session.originalVersion) {
            val message = "The current version [$currentVersion] should be [${session.originalVersion}]"
            log.debug(message)
            promise0.fail(CommandException.OptimisticLockingException(message))
          } else {
            log.debug("Version is {}", currentVersion)
            promise0.complete()
          }
        }
        .onFailure {
          log.error("When get snapshot", it)
          promise0.fail(it)
        }
      return promise0.future()
    }

    fun appendCommand(conn: SqlConnection): Future<Void> {
      if (!saveCommandOption) {
        return Future.succeededFuture(null)
      }
      val p = Promise.promise<Void>()
      val cmdAsJson = command.toJson(config.json)
      log.debug("Will append command {} as {}", command, cmdAsJson)
      val params = Tuple.of(
        metadata.commandId.id,
        JsonObject(cmdAsJson)
      )
      conn.preparedQuery(SQL_APPEND_CMD)
        .execute(params)
        .onFailure { p.fail(it) }
        .onSuccess {
          p.complete()
        }
      return p.future()
    }

    fun appendEvents(conn: SqlConnection): Future<Int> {
      fun appendEvent(event: E, version: Int, causationId: UUID): Future<Pair<Boolean, UUID>> {
        val aePromise = Promise.promise<Pair<Boolean, UUID>>()
        val eventAsJson = event.toJson(config.json)
        log.debug("Will append event {} as {}", event, eventAsJson)
        val params = Tuple.of(
          JsonObject(eventAsJson),
          config.name,
          metadata.aggregateRootId.id,
          version,
          metadata.correlationId.id,
          causationId
        )
        conn.preparedQuery(SQL_APPEND_EVENT)
          .execute(params)
          .onFailure { aePromise.fail(it) }
          .onSuccess { rs ->
            aePromise.complete(Pair(true, rs.first().getUUID("id")))
            log.debug("Append event ok {}", event)
          }
        return aePromise.future()
      }
      fun <A, B> foldLeft(iterator: Iterator<A>, identity: B, bf: BiFunction<B, A, B>): B {
        var result = identity
        while (iterator.hasNext()) {
          val next = iterator.next()
          result = bf.apply(result, next)
        }
        return result
      }

      val aesPromise = Promise.promise<Int>()
      val initialFuture = Future.succeededFuture(Pair(true, metadata.commandId.id))
      val version = AtomicInteger(session.originalVersion)
      foldLeft(
        session.appliedEvents().iterator(), initialFuture,
        { currentFuture: Future<Pair<Boolean, UUID>>,
          event: E ->
          currentFuture.compose { pair ->
            if (pair.first) {
              appendEvent(event, version.incrementAndGet(), pair.second)
            } else {
              Future.failedFuture("The latest successful event was $event")
            }
          }
        }
      ).onComplete {
        if (it.failed()) {
          aesPromise.fail(it.cause())
        } else {
          aesPromise.complete(version.get())
        }
      }
      return aesPromise.future()
    }

    fun updateVersion(conn: SqlConnection, resultingVersion: Int): Future<Void> {
      val promise = Promise.promise<Void>()
      val newSTateAsJson = session.currentState.toJson(config.json)
      log.debug("Will append snapshot {}", newSTateAsJson)
      if (session.originalVersion == 0) {
        val params = Tuple.of(
          resultingVersion,
          JsonObject(newSTateAsJson),
          metadata.aggregateRootId.id,
          config.name
        )
        conn.preparedQuery(SQL_INSERT_VERSION).execute(params)
          .onSuccess { promise.complete() }
          .onFailure { promise.fail(it) }
      } else {
        val params = Tuple.of(
          resultingVersion,
          JsonObject(newSTateAsJson),
          metadata.aggregateRootId.id,
          session.originalVersion
        )
        conn.preparedQuery(SQL_UPDATE_VERSION).execute(params)
          .onSuccess { promise.complete() }
          .onFailure { promise.fail(it) }
      }
      return promise.future()
    }

    val promise = Promise.promise<Void>()
    writeModelDb.connection
      .onFailure {
        promise.fail(it)
      }
      .onSuccess { conn ->
        conn.begin()
          .onFailure { err ->
            promise.fail(err)
            close(conn)
          }
          .onSuccess { tx: Transaction ->
            lockSnapshotIfVersionMatches(conn)
              .onFailure { err ->
                rollback(tx, err)
                close(conn)
                promise.fail(err)
              }
              .onSuccess {
                appendCommand(conn)
                  .onFailure { err ->
                    rollback(tx, err)
                    close(conn)
                    promise.fail(err)
                  }
                  .onSuccess {
                    appendEvents(conn)
                      .onFailure { err ->
                        rollback(tx, err)
                        close(conn)
                        promise.fail(err)
                      }
                      .onSuccess { resultingVersion ->
                        updateVersion(conn, resultingVersion)
                          .onFailure { err ->
                            rollback(tx, err)
                            close(conn)
                            promise.fail(err)
                          }
                          .onSuccess {
                            commit(tx)
                              .onFailure { err ->
                                rollback(tx, err)
                                close(conn)
                                promise.fail(err)
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
      }
    return promise.future()
  }
}
