package io.github.crabzilla.cassandra

import com.datastax.oss.driver.api.core.cql.BatchStatement
import com.datastax.oss.driver.api.core.cql.BatchType
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import com.github.f4b6a3.uuid.UuidCreator
import io.github.crabzilla.core.AGGREGATE_ROOT_SERIALIZER
import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.AggregateRootConfig
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DOMAIN_EVENT_SERIALIZER
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.StatefulSession
import io.github.crabzilla.stack.CommandMetadata
import io.github.crabzilla.stack.EventStore
import io.vertx.cassandra.CassandraClient
import io.vertx.cassandra.ResultSet
import io.vertx.core.Future
import io.vertx.core.Promise
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * This is just a proof of concept: Cassandra doesn't have any transactional operation over more than 1 table
 */
class CassandraEventStore<A : AggregateRoot, C : Command, E : DomainEvent>(
  val topic: String,
  private val cassandra: CassandraClient,
  private val config: AggregateRootConfig<A, C, E>
) : EventStore<A, C, E> {

  /**
   TODO after committing events, it could use topic property to publish to it using eventbus.
   Just for observability, not for transactional consumers
   */

  companion object {
    private val log = LoggerFactory.getLogger(CassandraEventStore::class.java)
  }

  /**

   BEGIN BATCH;
   update example1.snapshots set version = 2, json_content = '{}' where ar_name = 'customer' and ar_id = 1 if version = 1;
   insert into example1.events (event_id, ar_id, ar_name, cmd_id, event_payload) values (497fb368-9fc5-11eb-bcbc-0242ac130002, 5, 'customer', 'x5', '{}');
   APPLY BATCH;

   *
   */
  override fun append(command: C, metadata: CommandMetadata, session: StatefulSession<A, E>): Future<Void> {

    fun tryToAppendVersionZero(): String {
      return """ insert into example1.snapshots (ar_name, ar_id, version)
                  values ('${config.name.value}', ${metadata.aggregateRootId}, 0)
                    if NOT EXISTS
      """.trim()
    }

    fun tryToUpdateStateAndVersion(): String {
      val newSTateAsJson: String = config.json.encodeToString(AGGREGATE_ROOT_SERIALIZER, session.currentState)
      return """ update example1.snapshots
                    set version = ${session.originalVersion + 1},
                        json_content = '$newSTateAsJson'
                  where ar_name = '${config.name.value}'
                    and ar_id = ${metadata.aggregateRootId}
                    if version = ${session.originalVersion}
      """.trim()
    }

    fun tryToAppendEvent(event: E): String {
      val json = config.json.encodeToString(DOMAIN_EVENT_SERIALIZER, event)
      val timeUuid: UUID = UuidCreator.getTimeBasedWithMac()
      return """ insert into example1.events (event_id, event_payload, ar_name, ar_id, version, cmd_id)
                      values ($timeUuid,
                      '$json',
                      '${config.name.value}',
                      ${metadata.aggregateRootId},
                      ${session.originalVersion + 1},
                      '${metadata.id}')
      """.trim()
    }

    fun isNew(resultSet: ResultSet): Boolean {
      return resultSet.wasApplied()
    }

    val promise = Promise.promise<Void>()
    // TODO problem https://stackoverflow.com/questions/40996347/cassandra-batch-with-conditions-cannot-span-multiple-tables

    cassandra.execute(tryToAppendVersionZero())
      .onFailure { promise.fail(it) }
      .onSuccess { resultSet: ResultSet ->
        val isNew = isNew(resultSet)
        println("""*** is new $isNew""")
        cassandra.execute(tryToUpdateStateAndVersion())
          .onFailure { promise.fail(it) }
          .onSuccess { rs ->
            if (rs.wasApplied()) {
              val batchStatement = BatchStatement.newInstance(BatchType.LOGGED)
                .addAll(session.appliedEvents().map { SimpleStatement.newInstance(tryToAppendEvent(it)) })
              println("""*** will append ${batchStatement.size()} events""")
              cassandra.execute(batchStatement).map { it.wasApplied() }
            } else {
              println("""*** will NOT append events""")
              Future.succeededFuture(false)
            }
              .onFailure {
                promise.fail(it)
                // TODO: should I rollback the snapshot change?
                log.error("Unable to append events. TODO: should I rollback the snapshot change?", it)
              }.onSuccess {
                if (it) {
                  promise.complete()
                  log.info("Events successfully appended")
                } else {
                  promise.fail("wasn't applied")
                  log.info("Events NOT successfully appended")
                }
              }
          }
      }

    return promise.future()
  }
}
