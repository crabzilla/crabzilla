package io.github.crabzilla.webpgc

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Vertx
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
      val httpPort = config.getInteger("HTTP_PORT")
      val nextFreeHttpPort = nextFreePort(httpPort, httpPort + 20)
      config.put("HTTP_PORT", nextFreeHttpPort)
      log.info("*** next free HTTP_PORT: $nextFreeHttpPort")
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

fun deploySingleton(vertx: Vertx, verticle: String, pingEndpoint: String,
                    dOpt: DeploymentOptions, processId: Any): Future<String> {
  val future: Future<String> = Future.future()
  vertx.eventBus().send<String>(DbProjectionsVerticle.amIAlreadyRunning(pingEndpoint), processId) { isWorking ->
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


