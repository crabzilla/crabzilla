package io.github.crabzilla.pgclient.command

import io.github.crabzilla.core.command.CommandControllerConfig
import io.github.crabzilla.pgclient.EventsProjector
import io.github.crabzilla.pgclient.command.internal.OnDemandSnapshotRepo
import io.github.crabzilla.pgclient.command.internal.PersistentSnapshotRepo
import io.github.crabzilla.pgclient.command.internal.SnapshotRepository
import io.vertx.core.Vertx
import io.vertx.pgclient.PgPool
import kotlinx.serialization.json.Json

class CommandControllerBuilder(val vertx: Vertx, val pgPool: PgPool) {

  fun <S: Any, C: Any, E: Any> build(
    json: Json,
    config: CommandControllerConfig<S, C, E>,
    snapshotType: SnapshotType,
    eventsProjector: EventsProjector? = null
  ): CommandController<S, C, E> {
    fun <S: Any, C: Any, E: Any> snapshotRepo(
      snapshotType: SnapshotType,
      config: CommandControllerConfig<S, C, E>
    ): SnapshotRepository<S, E> {
      return when (snapshotType) {
        SnapshotType.ON_DEMAND -> OnDemandSnapshotRepo(config.eventHandler, json, config.eventSerDer)
        SnapshotType.PERSISTENT -> PersistentSnapshotRepo(config.stateSerDer, json)
      }
    }
    return CommandController(vertx, pgPool, json, config, snapshotRepo(snapshotType, config), eventsProjector)
  }

}