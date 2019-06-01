package io.github.crabzilla.web

import io.github.crabzilla.CommandMetadata
import io.github.crabzilla.Entity
import io.github.crabzilla.EntityComponent
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.kotlin.core.json.JsonArray
import org.slf4j.LoggerFactory

class WebEntityComponent<E: Entity>(private val resourceName: String, private val component: EntityComponent<E>) {

  private val postCmd = "/$resourceName/:$ENTITY_ID_PARAMETER/commands/:$COMMAND_NAME_PARAMETER"
  private val getSnapshot = "/$resourceName/:$ENTITY_ID_PARAMETER"
  private val getAllUow = "/$resourceName/:$ENTITY_ID_PARAMETER/units-of-work"
  private val getUow = "/$resourceName/units-of-work/:unitOfWorkId"

  companion object {
    private const val COMMAND_NAME_PARAMETER = "commandName"
    private const val COMMAND_ID_PARAMETER = "commandId"
    private const val ENTITY_ID_PARAMETER = "entityId"
    private const val UNIT_OF_WORK_ID_PARAMETER = "unitOfWorkId"
    private val log = LoggerFactory.getLogger(WebEntityComponent::class.java)
  }

  fun deployWebRoutes(router: Router) {

    log.info("adding route $postCmd")

    router.post(postCmd).handler {
      val commandMetadata = CommandMetadata(it.pathParam(ENTITY_ID_PARAMETER).toInt(),
                                            it.pathParam(COMMAND_NAME_PARAMETER))
      val begin = System.currentTimeMillis()
      component.handleCommand(commandMetadata, it.bodyAsJson, Handler { event ->
        val end = System.currentTimeMillis()
        log.info("received response in " + (end - begin) + " ms")
        if (event.succeeded()) {
          with(event.result()) {
            val location = it.request().absoluteURI().split('/').subList(0, 3)
              .reduce { acc, s ->  acc.plus("/$s")} + "/$resourceName/units-of-work/$second"
            it.response()
              .putHeader("accept", "application/json")
              .putHeader("Location", location)
              .setStatusCode(303)
              .end()
          }
        } else {
          it.response().setStatusCode(400).setStatusMessage(event.cause().message).end()
        }
      })
    }

    log.info("adding route $getSnapshot")

    router.get(getSnapshot).handler {
      val entityId = it.pathParam(ENTITY_ID_PARAMETER).toInt()
      println("retrieving write model state for $entityId")
      val httpResp = it.response()
      component.getSnapshot(entityId, Handler { event ->
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
      })
    }

    log.info("adding route $getAllUow")

    router.get(getAllUow).handler {
      val entityId = it.pathParam(ENTITY_ID_PARAMETER).toInt()
      println("retrieving uows for $entityId")
      val httpResp = it.response()
      component.getAllUowByEntityId(entityId, Handler { event ->
        if (event.failed() || event.result() == null) {
          httpResp.statusCode = if (event.result() == null) 404 else 500
          httpResp.end()
        } else {
          val resultList = event.result()
          httpResp.setStatusCode(200).setChunked(true)
            .headers().add("Content-Type", "application/json")
          httpResp.end(JsonArray(resultList).encode())
        }
      })
    }

    log.info("adding route $getUow")

    router.get(getUow).handler {
      val uowId = it.pathParam(UNIT_OF_WORK_ID_PARAMETER).toLong()
      println("retrieving uow $uowId")
      val httpResp = it.response()
      component.getUowByUowId(uowId, Handler { uowResult ->
        if (uowResult.failed() || uowResult.result() == null) {
          httpResp.statusCode = if (uowResult.result() == null) 404 else 500
          httpResp.end()
        } else {
          httpResp.setStatusCode(200).setChunked(true)
            .putHeader("Content-Type", "application/json")
            .putHeader("uowId", uowId.toString())
            .end(JsonObject.mapFrom(uowResult.result()).encode())
        }
      })
    }

  }
}
