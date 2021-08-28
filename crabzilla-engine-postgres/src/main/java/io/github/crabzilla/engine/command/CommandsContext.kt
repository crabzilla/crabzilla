package io.github.crabzilla.engine.command

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State
import io.github.crabzilla.core.command.CommandControllerConfig
import io.github.crabzilla.engine.projector.EventsProjector
import io.github.crabzilla.serder.SerDer
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient

class CommandsContext(val vertx: Vertx, val serDer: SerDer, val pgPool: PgPool, val sqlClient: SqlClient) {

  companion object {
    fun create(vertx: Vertx, serDer: SerDer, connectOptions: PgConnectOptions, poolOptions: PoolOptions): CommandsContext {
      val thePgPool: PgPool = PgPool.pool(vertx, connectOptions, poolOptions)
      val theSqlClient: SqlClient = PgPool.client(vertx, connectOptions, poolOptions)
      return CommandsContext(vertx, serDer, thePgPool, theSqlClient)
    }
  }

  /**
   * Creates a CommandController
   */
  fun <S : State, C : Command, E : Event> create(
    config: CommandControllerConfig<S, C, E>
  ): CommandController<S, C, E> {
    return CommandController(
      vertx, config, pgPool, serDer,
      saveCommandOption = true,
      advisoryLockOption = true,
      eventsProjector = null
    )
  }

  /**
   * Creates a more configurable CommandController
   */
  fun <S : State, C : Command, E : Event> create(
    config: CommandControllerConfig<S, C, E>,
    saveCommandOption: Boolean,
    advisoryLockOption: Boolean,
    eventsProjector: EventsProjector?
  ): CommandController<S, C, E> {
    return CommandController(
      vertx, config, pgPool, serDer,
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
