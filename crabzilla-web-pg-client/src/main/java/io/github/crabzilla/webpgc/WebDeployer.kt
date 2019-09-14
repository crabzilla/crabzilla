package io.github.crabzilla.webpgc

import io.github.crabzilla.framework.Command
import io.github.crabzilla.framework.CommandMetadata
import io.github.crabzilla.framework.Entity
import io.github.crabzilla.internal.EntityComponent
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import org.slf4j.LoggerFactory
import java.util.*

class WebDeployer<E : Entity>(private val component: EntityComponent<E>,
                              private val resourceName: String,
                              private val router: Router) {

  private val postCmd = "/commands/$resourceName/:$ENTITY_ID_PARAMETER/:$COMMAND_NAME_PARAMETER"
  private val getSnapshot = "/commands/$resourceName/:$ENTITY_ID_PARAMETER"
  private val getAllUow = "/commands/$resourceName/:$ENTITY_ID_PARAMETER/units-of-work"
  private val getUow = "/commands/$resourceName/units-of-work/:unitOfWorkId"

  companion object {
    const val COMMAND_NAME_PARAMETER = "commandName"
    const val COMMAND_ID_PARAMETER = "commandId"
    const val ENTITY_ID_PARAMETER = "entityId"
    const val UNIT_OF_WORK_ID_PARAMETER = "unitOfWorkId"
    private val log = LoggerFactory.getLogger(WebDeployer::class.java)

  }

  fun deployWebRoutes() {

    log.info("adding route POST $postCmd")
    router.post(postCmd).handler {
      val begin = System.currentTimeMillis()
      val commandId = it.request().getHeader(COMMAND_ID_PARAMETER)
      val commandMetadata =
        if (commandId == null) {
          CommandMetadata(it.pathParam(ENTITY_ID_PARAMETER).toInt(), it.pathParam(COMMAND_NAME_PARAMETER))
        } else {
          CommandMetadata(it.pathParam(ENTITY_ID_PARAMETER).toInt(), it.pathParam(COMMAND_NAME_PARAMETER),
            UUID.fromString(commandId))
        }
      val command: Command? = try {
        component.cmdFromJson(commandMetadata.commandName, it.bodyAsJson)
      } catch (e: Exception) {
        null
      }
      if (command == null) {
        it.response().setStatusCode(400).setStatusMessage("Cannot decode the json for this Command").end()
        return@handler
      }
      if (log.isTraceEnabled) log.trace("Handling $command $commandMetadata")
      component.handleCommand(commandMetadata, command).future().setHandler { event ->
        val end = System.currentTimeMillis()
        if (log.isTraceEnabled) log.trace("handled command in " + (end - begin) + " ms")
        if (event.succeeded()) {
          with(event.result()) {
            val location = it.request().absoluteURI().split('/').subList(0, 3)
              .reduce { acc, s -> acc.plus("/$s") } + "/commands/$resourceName/units-of-work/$second"
            it.response()
              .putHeader("accept", "application/json")
              .putHeader("Location", location)
              .setStatusCode(303)
              .end()
          }
        } else {
          log.error(event.cause().message)
          it.response().setStatusCode(400).setStatusMessage(event.cause().message).end()
        }
      }
    }.failureHandler(errorHandler(ENTITY_ID_PARAMETER))

    log.info("adding route GET $getSnapshot")

    router.get(getSnapshot).handler {
      val entityId = it.pathParam(ENTITY_ID_PARAMETER).toInt()
      val httpResp = it.response()
      component.getSnapshot(entityId).future().setHandler { event ->
        if (event.failed() || event.result() == null) {
          httpResp.statusCode = if (event.result() == null) 404 else 500
          httpResp.end()
        } else {
          val snapshot = event.result()
          val snapshotJson = JsonObject()
            .put("state", component.toJson(snapshot.state))
            .put("version", snapshot.version)
          if (snapshot.version > 0) {
            httpResp.headers().add("Content-Type", "application/json")
            httpResp.end(snapshotJson.encode())
          } else {
            httpResp.setStatusCode(404).end("Entity not found")
          }
        }
      }
    }.failureHandler(errorHandler(ENTITY_ID_PARAMETER))

    log.info("adding route GET $getAllUow")

    router.get(getAllUow).handler {
      val entityId = it.pathParam(ENTITY_ID_PARAMETER).toInt()
      val httpResp = it.response()
      component.getAllUowByEntityId(entityId).future().setHandler { event ->
        if (event.failed() || event.result() == null) {
          httpResp.statusCode = if (event.result() == null) 404 else 500
          httpResp.end()
        } else {
          val resultList = event.result()
          httpResp.setStatusCode(200).setChunked(true)
            .headers().add("Content-Type", "application/json")
          httpResp.end(JsonArray(resultList).encode())
        }
      }
    }.failureHandler(errorHandler(ENTITY_ID_PARAMETER))

    log.info("adding route GET $getUow")

    router.get(getUow).handler {
      val uowId = it.pathParam(UNIT_OF_WORK_ID_PARAMETER).toLong()
      val httpResp = it.response()
      component.getUowByUowId(uowId).future().setHandler { uowResult ->
        if (uowResult.failed() || uowResult.result() == null) {
          httpResp.statusCode = if (uowResult.result() == null) 404 else 500
          httpResp.end()
        } else {
          httpResp.setStatusCode(200).setChunked(true)
            .putHeader("Content-Type", "application/json")
            .putHeader("uowId", uowId.toString())
            .end(JsonObject.mapFrom(uowResult.result()).encode())
        }
      }
    }.failureHandler(errorHandler(UNIT_OF_WORK_ID_PARAMETER))

  }

}
