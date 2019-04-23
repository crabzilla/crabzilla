package io.github.crabzilla

import io.github.crabzilla.JsonMetadata.COMMAND_ID
import io.github.crabzilla.JsonMetadata.COMMAND_JSON_CONTENT
import io.github.crabzilla.JsonMetadata.COMMAND_NAME
import io.github.crabzilla.JsonMetadata.COMMAND_ENTITY_ID
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.util.*

typealias CommandHandlerFactory<E> =
  (Int, String, UUID, String, Command, Snapshot<E>, Handler<AsyncResult<UnitOfWork>>) -> CommandHandler<E>


abstract class CommandHandler<E: Entity>(val entityId: Int,
                                         val entityName: String,
                                         val commandId: UUID,
                                         val commandName: String,
                                         val command: Command,
                                         val snapshot: Snapshot<E>,
                                         val stateFn: (DomainEvent, E) -> E,
                                         uowHandler: Handler<AsyncResult<UnitOfWork>>) {
  val uowFuture: Future<UnitOfWork> = Future.future()
  val eventsFuture: Future<List<DomainEvent>> = Future.future()
  init {
    uowFuture.setHandler(uowHandler)
    eventsFuture.setHandler { event ->
      if (event.succeeded()) {
        uowFuture.complete(
          UnitOfWork.of(entityId, entityName, commandId, commandName, command, event.result(), snapshot.version + 1))
      } else {
        uowFuture.fail(event.cause())
      }
    }
  }
  abstract fun handleCommand()
}

class CommandHandlerEndpoint(val entityName : String) {
  fun endpoint() : String {
    return "$entityName-command-handler"
  }
}

class CommandHandlerVerticle<E : Entity>(val endpoint: CommandHandlerEndpoint,
                                         private val jsonToCommand: (String, JsonObject) -> Command,
                                         private val seedValue: E,
                                         private val cmdHandlerFactory: CommandHandlerFactory<E>,
                                         private val validatorFn: (Command) -> List<String>,
                                         private val eventJournal: UnitOfWorkJournal,
                                         private val snapshotRepo: SnapshotRepository<E>)
  : AbstractVerticle() {

  companion object {
    internal val log = LoggerFactory.getLogger(CommandHandlerVerticle::class.java)
  }

  @Throws(Exception::class)
  override fun start() {

    log.info("starting command handler verticle for endpoint: $endpoint")

    vertx.eventBus().consumer<JsonObject>(endpoint.endpoint(), Handler { commandEvent ->

      val cmdJsonMetadata = commandEvent.body()
      log.info("received a command $cmdJsonMetadata")

      val targetId: Int? = cmdJsonMetadata.getInteger(COMMAND_ENTITY_ID)

      if (targetId == null) {
        commandEvent.fail(400, "Put command handler must receive a COMMAND_TARGET_ID")
        return@Handler
      }

      val commandId = UUID.fromString(cmdJsonMetadata.getString(COMMAND_ID))
      val commandName = cmdJsonMetadata.getString(COMMAND_NAME)
      val commandJson = cmdJsonMetadata.getJsonObject(COMMAND_JSON_CONTENT)

      val command: Command? = try { jsonToCommand.invoke(commandName, commandJson) } catch (e: Exception) { null }

      if (command == null) {
        commandEvent.fail(400, "Command cannot be deserialized")
        return@Handler
      }

      val constraints = validatorFn.invoke(command)

      if (constraints.isNotEmpty()) {
        commandEvent.fail(400, constraints.toString())
        return@Handler
      }

      val resultFuture : Future<Pair<UnitOfWork, Int>> = Future.future()

      resultFuture.setHandler { event ->
        if (event.failed()) {
          commandEvent.fail(400, event.cause().message)
        } else {
          commandEvent.reply(event.result())
        }
      }

      val snapshotFuture: Future<Snapshot<E>> = Future.future()

      snapshotRepo.retrieve(targetId, endpoint.endpoint(), snapshotFuture)

      var resultingUow : UnitOfWork? = null

      snapshotFuture

        .compose { snapshot ->
          val commandHandlerFuture = Future.future<UnitOfWork>()
          val cachedSnapshot = snapshot ?: Snapshot(seedValue, 0)
          val cmdHandler = cmdHandlerFactory.invoke(targetId, endpoint.entityName, commandId, commandName, command,
            cachedSnapshot, commandHandlerFuture)
          cmdHandler.handleCommand()
          commandHandlerFuture
        }

        .compose { unitOfWork ->
          resultingUow = unitOfWork
          val appendFuture = Future.future<Int>()
          eventJournal.append(unitOfWork, appendFuture.completer())
          appendFuture
        }

        .compose({ uowSequence ->
          val pair: Pair<UnitOfWork, Int> = Pair<UnitOfWork, Int>(resultingUow!!, uowSequence)
          resultFuture.complete(pair)
        }, resultFuture)

    })

  }

}
