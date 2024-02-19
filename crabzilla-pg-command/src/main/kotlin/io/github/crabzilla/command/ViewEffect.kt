package io.github.crabzilla.command

import io.github.crabzilla.context.EventMetadata
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.SqlConnection

sealed interface ViewEffect<S : Any, E : Any> {
  interface GivenEachEventViewEffect<S : Any, E : Any> : ViewEffect<S, E> {
    fun handle(
      sqlConnection: SqlConnection,
      event: E,
      state: S,
      eventMetadata: EventMetadata,
    ): Future<JsonObject?>
  }

  interface GivenAllEventsViewEffect<S : Any, E : Any> : ViewEffect<S, E> {
    fun handle(
      sqlConnection: SqlConnection,
      result: CommandHandlerResult<S, E>,
    ): Future<JsonObject?>
  }
}
