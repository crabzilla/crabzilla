package io.github.crabzilla.cassandra

import com.datastax.oss.driver.api.core.cql.Row
import io.github.crabzilla.core.AGGREGATE_ROOT_SERIALIZER
import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.AggregateRootConfig
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.Snapshot
import io.github.crabzilla.stack.SnapshotRepository
import io.vertx.cassandra.CassandraClient
import io.vertx.core.Future
import io.vertx.core.Promise

class CassandraSnapshotRepo<A : AggregateRoot, C : Command, E : DomainEvent>(
  private val config: AggregateRootConfig<A, C, E>,
  private val cassandra: CassandraClient
) : SnapshotRepository<A, C, E> {

  override fun get(id: Int): Future<Snapshot<A>?> {
    val promise = Promise.promise<Snapshot<A>?>()
    cassandra.executeWithFullFetch("select version, json_content from example1.snapshots where ar_id = $id") { executeWithFullFetch ->
      if (executeWithFullFetch.succeeded()) {
        val rows: List<Row> = executeWithFullFetch.result()
        when (rows.size) {
          0 -> promise.complete(null)
          1 -> {
            val version = rows[0].getInt(0)
            val json = rows[0].getString(1)
            val state = config.json.decodeFromString(AGGREGATE_ROOT_SERIALIZER, json!!) as A
            promise.complete(Snapshot(state, version))
          }
          else -> promise.fail("It should return 0 or 1 snapshot")
        }
      } else {
        promise.fail(executeWithFullFetch.cause())
      }
    }
    return promise.future()
  }

  override fun upsert(id: Int, snapshot: Snapshot<A>): Future<Void> {
    TODO("Not yet implemented")
  }
}
