package io.github.crabzilla.pgc.command

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.CommandControllerConfig
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.DomainState
import io.github.crabzilla.pgc.projector.EventsProjector
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient
import kotlinx.serialization.json.Json

class CommandsContext(val vertx: Vertx, val json: Json, val pgPool: PgPool, val sqlClient: SqlClient) {

  companion object {
    fun create(vertx: Vertx, json: Json, connectOptions: PgConnectOptions, poolOptions: PoolOptions): CommandsContext {
      val thePgPool: PgPool = PgPool.pool(vertx, connectOptions, poolOptions)
      val theSqlClient: SqlClient = PgPool.client(vertx, connectOptions, poolOptions)
      return CommandsContext(vertx, json, thePgPool, theSqlClient)
    }
  }

  /**
   * Creates a DefaultCommandController
   */
  fun <A : DomainState, C : Command, E : DomainEvent> create(
    config: CommandControllerConfig<A, C, E>
  ): DefaultCommandController<A, C, E> {
    return DefaultCommandController(
      config, pgPool, json,
      saveCommandOption = true,
      advisoryLockOption = true,
      eventsProjector = null
    )
  }

  /**
   * Creates a more configurable DefaultCommandController
   */
  fun <A : DomainState, C : Command, E : DomainEvent> create(
    config: CommandControllerConfig<A, C, E>,
    saveCommandOption: Boolean,
    advisoryLockOption: Boolean,
    eventsProjector: EventsProjector?
  ): DefaultCommandController<A, C, E> {
    return DefaultCommandController(
      config, pgPool, json,
      saveCommandOption,
      advisoryLockOption,
      eventsProjector
    )
  }

  fun close(): Future<Void> {
    return pgPool.close()
      .compose { sqlClient.close() }
  }
}
