package io.github.crabzilla.engine.command

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State
import io.github.crabzilla.core.command.CommandControllerConfig
import io.github.crabzilla.core.serder.JsonSerDer
import io.github.crabzilla.engine.command.SnapshotRepository.SnapshotType
import io.github.crabzilla.engine.command.SnapshotRepository.SnapshotType.ON_DEMAND
import io.github.crabzilla.engine.command.SnapshotRepository.SnapshotType.PERSISTENT
import io.github.crabzilla.engine.projector.EventsProjector
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient

class CommandsContext(val vertx: Vertx, val jsonSerDer: JsonSerDer, val pgPool: PgPool, val sqlClient: SqlClient) {

  companion object {
    fun create(
      vertx: Vertx,
      jsonSerDer: JsonSerDer,
      connectOptions: PgConnectOptions,
      poolOptions: PoolOptions
    ): CommandsContext {
      val thePgPool: PgPool = PgPool.pool(vertx, connectOptions, poolOptions)
      val theSqlClient: SqlClient = PgPool.client(vertx, connectOptions, poolOptions)
      return CommandsContext(vertx, jsonSerDer, thePgPool, theSqlClient)
    }
  }

  /**
   * Creates a CommandController
   */
  fun <S : State, C : Command, E : Event> create(
    config: CommandControllerConfig<S, C, E>,
    snapshotType: SnapshotType
  ): CommandController<S, C, E> {
    return CommandController(vertx, config, pgPool, jsonSerDer, snapshotRepo(snapshotType, config))
  }

  /**
   * Creates a more configurable CommandController
   */
  fun <S : State, C : Command, E : Event> create(
    config: CommandControllerConfig<S, C, E>,
    snapshotType: SnapshotType,
    eventsProjector: EventsProjector?
  ): CommandController<S, C, E> {
    return CommandController(vertx, config, pgPool, jsonSerDer, snapshotRepo(snapshotType, config), eventsProjector)
  }

  fun close(): Future<Void> {
    return pgPool.close()
      .compose { sqlClient.close() }
  }

  private fun <S : State, C : Command, E : Event> snapshotRepo(
    snapshotType: SnapshotType,
    config: CommandControllerConfig<S, C, E>
  ): SnapshotRepository<S, E> {
    return when (snapshotType) {
      ON_DEMAND -> OnDemandSnapshotRepo(config.eventHandler, jsonSerDer)
      PERSISTENT -> PersistentSnapshotRepo(config.name, jsonSerDer)
    }
  }
}
