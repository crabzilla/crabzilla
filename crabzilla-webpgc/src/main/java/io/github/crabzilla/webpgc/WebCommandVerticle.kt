package io.github.crabzilla.webpgc

import io.github.crabzilla.*
import io.github.crabzilla.pgc.PgcCmdHandler
import io.github.crabzilla.pgc.writeModelPgPool
import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.pgclient.PgPool
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

abstract class WebCommandVerticle : AbstractVerticle() {

  companion object {
    val log: Logger = LoggerFactory.getLogger(WebCommandVerticle::class.java)
  }

  val writeDb : PgPool by lazy { writeModelPgPool(vertx, config()) }
  val jsonFunctions: MutableMap<String, EntityJsonAware<out Entity>> = mutableMapOf()
  val eventsPublisher: UnitOfWorkPublisher by lazy { EventBusUowPublisher(vertx, jsonFunctions) }
  val httpPort : Int by lazy { config().getInteger("WRITE_HTTP_PORT")}

  fun <E: Entity> addResourceForEntity(resourceName: String, entityName: String,
                                       jsonAware: EntityJsonAware<E>, cmdAware: EntityCommandAware<E>,
                                       router: Router) {
    log.info("adding web command handler for entity $entityName on resource $resourceName")
    jsonFunctions[entityName] = jsonAware
    val cmdHandlerComponent = PgcCmdHandler(writeDb, entityName, jsonAware, cmdAware, eventsPublisher)
    WebDeployer(cmdHandlerComponent, resourceName, router).deployWebRoutes()
  }

}

private class WebDeployer<E: Entity>(private val component: EntityComponent<E>,
                             private val resourceName: String,
                             private val router: Router)
{

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
      val command: Command? = try { component.cmdFromJson(commandMetadata.commandName, it.bodyAsJson) }
                            catch (e: Exception) { null }
      if (command == null) {
        it.response().setStatusCode(400).setStatusMessage("Cannot decode the json for this Command").end()
        return@handler
      }
      log.info("Handling $command  $commandMetadata")
      component.handleCommand(commandMetadata, command, Handler { event ->
        val end = System.currentTimeMillis()
        log.info("handled command in " + (end - begin) + " ms")
        if (event.succeeded()) {
          with(event.result()) {
            val location = it.request().absoluteURI().split('/').subList(0, 3)
              .reduce { acc, s ->  acc.plus("/$s")} + "/commands/$resourceName/units-of-work/$second"
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
      })
    }.failureHandler(errorHandler(ENTITY_ID_PARAMETER))

    log.info("adding route GET $getSnapshot")

    router.get(getSnapshot).handler {
      val entityId = it.pathParam(ENTITY_ID_PARAMETER).toInt()
      val accept = it.request().getHeader("accept")
      if (ContentTypes.ENTITY_WRITE_MODEL == accept) {
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
      } else {
        it.next()
      }
    }.failureHandler(errorHandler(ENTITY_ID_PARAMETER))

    log.info("adding route GET $getAllUow")

    router.get(getAllUow).handler {
      val entityId = it.pathParam(ENTITY_ID_PARAMETER).toInt()
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
    }.failureHandler(errorHandler(ENTITY_ID_PARAMETER))

    log.info("adding route GET $getUow")

    router.get(getUow).handler {
      val uowId = it.pathParam(UNIT_OF_WORK_ID_PARAMETER).toLong()
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
    }.failureHandler(errorHandler(UNIT_OF_WORK_ID_PARAMETER))
  }

  private fun errorHandler(paramName: String) : Handler<RoutingContext> {
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

}
