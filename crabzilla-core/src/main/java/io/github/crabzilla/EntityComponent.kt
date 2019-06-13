package io.github.crabzilla

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface EntityComponent<E: Entity> {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(EntityComponent::class.java)
    fun cmdHandlerEndpoint(entityName: String): String {
      return entityName + "cmd-endpoint"
    }
  }

  fun entityName() : String

  fun handleCommand(metadata: CommandMetadata, command: Command, aHandler: Handler<AsyncResult<Pair<UnitOfWork, Long>>>)

  fun getUowByUowId(uowId: Long, aHandler: Handler<AsyncResult<UnitOfWork>>)

  fun getAllUowByEntityId(id: Int, aHandler: Handler<AsyncResult<List<UnitOfWork>>>)

  fun getSnapshot(entityId: Int, aHandler: Handler<AsyncResult<Snapshot<E>>>)

  fun toJson(state: E): JsonObject

  fun cmdFromJson(commandName: String, cmdAsJson: JsonObject): Command

  /**
   * Deploy command handling for this entity exposing it as a EventBus endpoint
   */
  fun deployCommandHandler(vertx: Vertx) {
    val endpoint = cmdHandlerEndpoint(entityName())
    log.info("deploying [$endpoint]")
    vertx.eventBus().consumer<Pair<CommandMetadata, Command>>(endpoint) { msg ->
      val pair = msg.body()
      val begin = System.currentTimeMillis()
      handleCommand(pair.first, pair.second, Handler { event ->
        val end = System.currentTimeMillis()
        log.trace("$endpoint handled command in " + (end - begin) + " ms")
        if (event.succeeded()) {
          msg.reply(event.result())
        } else {
          msg.fail(400, event.cause().message)
        }
      })
    }
  }

}

