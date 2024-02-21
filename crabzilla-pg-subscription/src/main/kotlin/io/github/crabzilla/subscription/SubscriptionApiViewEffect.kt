package io.github.crabzilla.subscription

import io.github.crabzilla.context.EventRecord
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection

interface SubscriptionApiViewEffect {
  fun handle(
    sqlConnection: SqlConnection,
    eventRecord: EventRecord,
  ): Future<JsonObject?>
}
