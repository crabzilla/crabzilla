package io.github.crabzilla.pgc.command

import io.github.crabzilla.core.AggregateRoot
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.pgc.projector.EventsProjector
import io.github.crabzilla.stack.AggregateRootConfig
import io.github.crabzilla.stack.CommandController
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient
import kotlinx.serialization.json.Json

class CommandControllerClient(val vertx: Vertx, val json: Json, val pgPool: PgPool, val sqlClient: SqlClient) {

  companion object {
    fun create(vertx: Vertx, json: Json, connectOptions: PgConnectOptions, poolOptions: PoolOptions): CommandControllerClient {
      val thePgPool: PgPool = PgPool.pool(vertx, connectOptions, poolOptions)
      val theSqlClient: SqlClient = PgPool.client(vertx, connectOptions, poolOptions)
      return CommandControllerClient(vertx, json, thePgPool, theSqlClient)
    }
  }

  /**
   * Creates a CommandController
   */
  fun <A : AggregateRoot, C : Command, E : DomainEvent> create(
    config: AggregateRootConfig<A, C, E>
  ): CommandController<A, C, E> {
    val snapshotRepo = PgcSnapshotRepo<A>(sqlClient, json)
    val eventStore = PgcEventStore(
      config, pgPool, json,
      saveCommandOption = true,
      optimisticLockOption = true,
      eventsProjectorApi = null
    )
    return CommandController(config, snapshotRepo, eventStore)
  }

  /**
   * Creates a more configurable CommandController
   */
  fun <A : AggregateRoot, C : Command, E : DomainEvent> create(
    config: AggregateRootConfig<A, C, E>,
    saveCommandOption: Boolean,
    optimisticLockOption: Boolean,
    projectorApi: EventsProjector?
  ): CommandController<A, C, E> {
    val snapshotRepo = PgcSnapshotRepo<A>(sqlClient, json)
    val eventStore = PgcEventStore(config, pgPool, json, saveCommandOption, optimisticLockOption, projectorApi)
    return CommandController(config, snapshotRepo, eventStore)
  }

  fun close(): Future<Void> {
    return pgPool.close()
      .compose { sqlClient.close() }
  }
}
