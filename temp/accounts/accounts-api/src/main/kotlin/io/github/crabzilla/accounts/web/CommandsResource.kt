package io.github.crabzilla.accounts.web

import io.github.crabzilla.core.Command
import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State
import io.github.crabzilla.core.command.CommandControllerConfig
import io.github.crabzilla.core.metadata.CommandMetadata
import io.github.crabzilla.json.KotlinJsonSerDer
import io.github.crabzilla.pgclient.EventsProjector
import io.github.crabzilla.pgclient.command.CommandController
import io.github.crabzilla.pgclient.command.CommandSideEffect
import io.github.crabzilla.pgclient.command.CommandsContext
import io.github.crabzilla.pgclient.command.SnapshotType
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID

internal class CommandsResource<S: State, C: Command, E: Event>(
  private val vertx: Vertx,
  private val config: JsonObject,
  private val json: Json,
  private val commandConfig: CommandControllerConfig<S, C, E>,
  private val eventsProjector: EventsProjector? = null) {

  companion object {
    const val ID_PARAM: String = "id"
    private val log = LoggerFactory.getLogger(CommandsResource::class.java)
  }

  fun handle(ctx: RoutingContext, commandFactory: (Pair<CommandMetadata, JsonObject>) -> C) {
    val (metadata, body) = requestHandler(ctx)
    commandController.value.handle(metadata, commandFactory.invoke(Pair(metadata, body)))
      .onSuccess { successHandler(ctx, it) }
      .onFailure { errorHandler(ctx, it) }
  }

  private val commandController: Lazy<CommandController<S, C, E>> = lazy {
    KotlinJsonSerDer(json)
  .let {
    CommandsContext.create(vertx, it, config.getJsonObject("accounts-db-config")) }
      .create(commandConfig, SnapshotType.ON_DEMAND, eventsProjector)
  }

  private fun requestHandler(ctx: RoutingContext) : Pair<CommandMetadata, JsonObject> {
    val id = UUID.fromString(ctx.request().getParam(ID_PARAM))
    val metadata = CommandMetadata(id)
    return Pair(metadata, ctx.bodyAsJson)
  }

  private fun successHandler(ctx: RoutingContext, data: CommandSideEffect) {
    ctx.response().setStatusCode(201).end(JsonObject.mapFrom(data).encode())
  }

  private fun errorHandler(ctx: RoutingContext, error: Throwable) {
    log.error(ctx.request().absoluteURI(), error)
    // a silly convention, but hopefully effective for this demo
    when (error.cause) {
      is IllegalArgumentException -> ctx.response().setStatusCode(400).setStatusMessage(error.message).end()
      is NullPointerException -> ctx.response().setStatusCode(404).setStatusMessage(error.message).end()
      is IllegalStateException -> ctx.response().setStatusCode(409).setStatusMessage(error.message).end()
      else -> ctx.response().setStatusCode(500).setStatusMessage(error.message).end()
    }
  }

}