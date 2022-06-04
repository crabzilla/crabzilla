package io.github.crabzilla.stack

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool

interface CrabzillaContextFactory {

  fun new(vertx: Vertx, pgConfig: JsonObject): CrabzillaContext

  fun new(vertx: Vertx, pgConfig: JsonObject, pgPool: PgPool): CrabzillaContext

}
