package io.github.crabzilla.pgclient.command

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State
import io.github.crabzilla.core.command.CommandControllerConfig
import io.github.crabzilla.core.json.JsonSerDer
import io.github.crabzilla.pgclient.EventsProjector
import io.github.crabzilla.pgclient.command.SnapshotType.ON_DEMAND
import io.github.crabzilla.pgclient.command.SnapshotType.PERSISTENT
import io.github.crabzilla.pgclient.command.internal.OnDemandSnapshotRepo
import io.github.crabzilla.pgclient.command.internal.OptionsFactory.createPgConnectOptions
import io.github.crabzilla.pgclient.command.internal.OptionsFactory.createPoolOptions
import io.github.crabzilla.pgclient.command.internal.PersistentSnapshotRepo
import io.github.crabzilla.pgclient.command.internal.SnapshotRepository
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions

class CommandsContext(val vertx: Vertx, val jsonSerDer: JsonSerDer, val pgPool: PgPool) {

  companion object {
    fun create(
      vertx: Vertx,
      jsonSerDer: JsonSerDer,
      connectOptions: PgConnectOptions,
      poolOptions: PoolOptions
    ): CommandsContext {
      val thePgPool: PgPool = PgPool.pool(vertx, connectOptions, poolOptions)
      return CommandsContext(vertx, jsonSerDer, thePgPool)
    }
    fun create(
      vertx: Vertx,
      jsonSerDer: JsonSerDer,
      config: JsonObject
    ): CommandsContext {
      val connectOptions = createPgConnectOptions(config)
      val poolOptions = createPoolOptions(config)
      val thePgPool: PgPool = PgPool.pool(vertx, connectOptions, poolOptions)
      return CommandsContext(vertx, jsonSerDer, thePgPool)
    }
  }

  /**
   * Creates a CommandController
   */
  fun <S : State, C : Command, E : Event> create(
    config: CommandControllerConfig<S, C, E>,
    snapshotType: SnapshotType
  ): CommandController<S, C, E> {
    return CommandController(vertx, pgPool, jsonSerDer, config, snapshotRepo(snapshotType, config),)
  }

  /**
   * Creates a more configurable CommandController
   */
  fun <S : State, C : Command, E : Event> create(
    config: CommandControllerConfig<S, C, E>,
    snapshotType: SnapshotType,
    eventsProjector: EventsProjector?
  ): CommandController<S, C, E> {
    return CommandController(vertx, pgPool, jsonSerDer, config, snapshotRepo(snapshotType, config), eventsProjector)
  }

  fun close(): Future<Void> {
    return pgPool.close()
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
