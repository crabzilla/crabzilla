package io.github.crabzilla.web.command

import io.github.crabzilla.core.command.AggregateRoot
import io.github.crabzilla.core.command.Command
import io.github.crabzilla.core.command.CommandMetadata
import io.vertx.core.Handler
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory
import java.util.UUID

class WebResourceDeployer<A : AggregateRoot>(
  private val resourceName: String,
  private val cmdTypeMap: Map<String, String>,
  private val helper: AggregateRootWebHelper<A>,
  private val router: Router
) {

  private val postCmd = "/commands/$resourceName/:$ENTITY_ID_PARAMETER/:$COMMAND_NAME_PARAMETER"
  private val getSnapshot = "/commands/$resourceName/:$ENTITY_ID_PARAMETER"
  private val getAllUow = "/commands/$resourceName/:$ENTITY_ID_PARAMETER/units-of-work"
  private val getUow = "/commands/$resourceName/units-of-work/:unitOfWorkId"

  companion object {
    const val COMMAND_NAME_PARAMETER = "commandName"
    const val COMMAND_ID_PARAMETER = "commandId"
    const val ENTITY_ID_PARAMETER = "entityId"
    const val UNIT_OF_WORK_ID_PARAMETER = "unitOfWorkId"
    const val JSON = "application/json"
    private val log = LoggerFactory.getLogger(WebResourceDeployer::class.java)
  }

  fun deployWebRoutes() {
    fun errorHandler(paramName: String): Handler<RoutingContext> {
      return Handler {
        log.error(it.failure().message, it.failure())
        when (it.failure()) {
          is NumberFormatException -> it.response().setStatusCode(400).end("path param $paramName must be a number")
          else -> {
            it.failure().printStackTrace()
            it.response().setStatusCode(500).end("server error")
          }
        }
      }
    }
    log.info("adding route POST $postCmd")
    router.post(postCmd).consumes(JSON).produces(JSON).handler {
      val begin = System.currentTimeMillis()
      val commandId = it.request().getHeader(COMMAND_ID_PARAMETER)
      val commandMetadata =
        if (commandId == null) {
          CommandMetadata(it.pathParam(ENTITY_ID_PARAMETER).toInt(), helper.entityName(),
                  it.pathParam(COMMAND_NAME_PARAMETER))
        } else {
          CommandMetadata(it.pathParam(ENTITY_ID_PARAMETER).toInt(), helper.entityName(),
                  it.pathParam(COMMAND_NAME_PARAMETER), UUID.fromString(commandId))
        }
      val commandType = cmdTypeMap[commandMetadata.commandName]
      val command: Command? = try {
        helper.cmdFromJson(it.bodyAsJson.put("type", commandType))
      } catch (e: Exception) {
        null
      }
      if (command == null) {
        it.response().setStatusCode(400).setStatusMessage("Cannot decode the json for this Command").end()
        return@handler
      }
      if (log.isDebugEnabled) log.debug("Handling $command $commandMetadata")
      helper.handleCommand(commandMetadata, command)
        .onSuccess { result ->
          val end = System.currentTimeMillis()
          if (log.isDebugEnabled) log.debug("handled command in " + (end - begin) + " ms")
          with(result) {
            val location = it.request().absoluteURI().split('/').subList(0, 3)
              .reduce { acc, s -> acc.plus("/$s") } + "/commands/$resourceName/units-of-work/$second"
            it.response()
              .putHeader("Location", location)
              .putHeader("Content-Type", JSON)
              .setStatusCode(303)
              .end()
          }
        }.onFailure { error ->
          log.error(error.message)
          it.response().setStatusCode(400).setStatusMessage(error.message).end()
        }
    }.failureHandler(errorHandler(ENTITY_ID_PARAMETER))

    log.info("adding route GET $getSnapshot")
    router.get(getSnapshot).produces(JSON).handler {
      val entityId = it.pathParam(ENTITY_ID_PARAMETER).toInt()
      val httpResp = it.response()
      helper.getSnapshot(entityId).onComplete { event ->
        if (event.failed() || event.result() == null) {
          httpResp.statusCode = if (event.result() == null) 404 else 500
          httpResp.end()
        } else {
          val snapshot = event.result()
          val snapshotJson = JsonObject()
            .put("state", helper.toJson(snapshot.state))
            .put("version", snapshot.version)
          if (snapshot.version > 0) {
            httpResp
              .putHeader("Content-Type", JSON)
              .end(snapshotJson.encode())
          } else {
            httpResp.setStatusCode(404).end("Entity not found")
          }
        }
      }
    }.failureHandler(errorHandler(ENTITY_ID_PARAMETER))

    log.info("adding route GET $getAllUow")
    router.get(getAllUow).produces(JSON).handler {
      val entityId = it.pathParam(ENTITY_ID_PARAMETER).toInt()
      val httpResp = it.response()
      helper.getAllUowByEntityId(entityId).onComplete { event ->
        if (event.failed() || event.result() == null) {
          httpResp.statusCode = if (event.result() == null) 404 else 500
          httpResp.end()
        } else {
          val resultList = event.result()
          httpResp.setStatusCode(200)
            .setChunked(true)
            .putHeader("Content-Type", JSON)
          httpResp.end(JsonArray(resultList).encode())
        }
      }
    }.failureHandler(errorHandler(ENTITY_ID_PARAMETER))

    log.info("adding route GET $getUow")
    router.get(getUow).produces(JSON).handler {
      val uowId = it.pathParam(UNIT_OF_WORK_ID_PARAMETER).toLong()
      val httpResp = it.response()
      helper.getUowByUowId(uowId).onComplete { uowResult ->
        if (uowResult.failed() || uowResult.result() == null) {
          httpResp.statusCode = if (uowResult.result() == null) 404 else 500
          httpResp.end()
        } else {
          httpResp.setStatusCode(200).setChunked(true)
            .putHeader("uowId", uowId.toString())
            .putHeader("Content-Type", JSON)
            .end(JsonObject.mapFrom(uowResult.result()).encode())
        }
      }
    }.failureHandler(errorHandler(UNIT_OF_WORK_ID_PARAMETER))
  }
}
