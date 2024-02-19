package io.github.crabzilla.context

import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection

interface ViewTrigger {
  fun checkCondition(viewAsJson: JsonObject): Boolean

  fun handleTrigger(
    sqlConnection: SqlConnection,
    viewAsJson: JsonObject,
  ): Future<Void>
}
