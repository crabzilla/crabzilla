package io.github.crabzilla.webpgc

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import io.github.crabzilla.*
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.*
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.ServerSocket

private val log = LoggerFactory.getLogger("webpgc")

fun getConfig(vertx: Vertx, configFile: String) : Future<JsonObject> {
  Json.mapper
    .registerModule(Jdk8Module())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
  val future: Future<JsonObject> = Future.future()
  configRetriever(vertx, configFile).getConfig { gotConfig ->
    if (gotConfig.succeeded()) {
      val config = gotConfig.result()
      log.info("*** config:\n${config.encodePrettily()}")
      val readHttpPort = config.getInteger("READ_HTTP_PORT")
      val nextFreeReadHttpPort = nextFreePort(readHttpPort, readHttpPort + 20)
      config.put("READ_HTTP_PORT", nextFreeReadHttpPort)
      log.info("*** next free READ_HTTP_PORT: $nextFreeReadHttpPort")
      val writeHttpPort = config.getInteger("WRITE_HTTP_PORT")
      val nextFreeWriteHttpPort = nextFreePort(writeHttpPort, writeHttpPort + 20)
      config.put("WRITE_HTTP_PORT", nextFreeWriteHttpPort)
      log.info("*** next free WRITE_HTTP_PORT: $nextFreeWriteHttpPort")
      future.complete(config)
    } else {
      future.fail(gotConfig.cause())
    }
  }
  return future
}

private fun configRetriever(vertx: Vertx, configFile: String): ConfigRetriever {
  val envOptions = ConfigStoreOptions()
    .setType("file")
    .setFormat("properties")
    .setConfig(JsonObject().put("path", configFile))
  val options = ConfigRetrieverOptions().addStore(envOptions)
  return ConfigRetriever.create(vertx, options)
}

fun deploy(vertx: Vertx, verticle: String, deploymentOptions: DeploymentOptions): Future<String> {
  val future: Future<String> = Future.future()
  vertx.deployVerticle(verticle, deploymentOptions, future)
  return future
}

fun deploySingleton(vertx: Vertx, verticle: String, dOpt: DeploymentOptions, processId: String): Future<String> {
  val future: Future<String> = Future.future()
  vertx.eventBus().send<String>(verticle, processId) { isWorking ->
    if (isWorking.succeeded()) {
      log.info("No need to start $verticle: " + isWorking.result().body())
    } else {
      log.info("*** Deploying $verticle")
      vertx.deployVerticle(verticle, dOpt) { wasDeployed ->
        if (wasDeployed.succeeded()) {
          log.info("$verticle started")
          future.complete("singleton ${wasDeployed.result()}")
        } else {
          log.error("$verticle not started", wasDeployed.cause())
          future.fail(wasDeployed.cause())
        }
      }
    }
  }
  return future
}

fun deployHandler(vertx: Vertx): Handler<AsyncResult<CompositeFuture>> {
  return Handler { deploys ->
    if (deploys.succeeded()) {
      val deploymentIds = deploys.result().list<String>()
      log.info("Verticles were successfully deployed")
      Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
          for (id in deploymentIds) {
            if (id.startsWith("singleton")) {
              log.info("Keeping singleton deployment $id")
            } else {
              log.info("Undeploying $id")
              vertx.undeploy(id)
            }
          }
          log.info("Closing vertx")
          vertx.close()
        }
      })
    } else {
      log.error("When deploying", deploys.cause())
    }
  }
}

private fun nextFreePort(from: Int, to: Int): Int {
  var port = from
  while (true) {
    if (isLocalPortFree(port)) {
      return port
    } else {
      if (port == to) {
        throw IllegalStateException("Could not find any from available from $from to $to");
      } else {
        port += 1
      }
    }
  }
}

private fun isLocalPortFree(port: Int): Boolean {
  return try {
    log.info("Trying port $port...")
    ServerSocket(port).close()
    true
  } catch (e: IOException) {
    false
  }
}

fun toUnitOfWorkEvents(json: JsonObject, jsonFunctions: Map<String, EntityJsonAware<out Entity>>): UnitOfWorkEvents? {

  val uowId = json.getLong("uowId")
  val entityName = json.getString(UnitOfWork.JsonMetadata.ENTITY_NAME)
  val entityId = json.getInteger(UnitOfWork.JsonMetadata.ENTITY_ID)
  val eventsArray = json.getJsonArray(UnitOfWork.JsonMetadata.EVENTS)

  val jsonAware = jsonFunctions[entityName]
  if (jsonAware == null) {
    log.error("JsonAware for $entityName wasn't found")
    return null
  }

  val jsonToEventPair: (Int) -> Pair<String, DomainEvent> = { index ->
    val jsonObject = eventsArray.getJsonObject(index)
    val eventName = jsonObject.getString(UnitOfWork.JsonMetadata.EVENT_NAME)
    val eventJson = jsonObject.getJsonObject(UnitOfWork.JsonMetadata.EVENTS_JSON_CONTENT)
    val domainEvent = jsonAware.eventFromJson(eventName, eventJson)
    domainEvent
  }

  val events: List<Pair<String, DomainEvent>> = List(eventsArray.size(), jsonToEventPair)
  return UnitOfWorkEvents(uowId, entityId, events)

}

object ContentTypes {
  const val UNIT_OF_WORK_ID = "application/vnd.crabzilla.unit-of-work-id+json"
  const val UNIT_OF_WORK_BODY = "application/vnd.crabzilla.unit-of-work+json"
  const val ENTITY_WRITE_MODEL = "application/vnd.crabzilla.entity-write-model+json"
}
