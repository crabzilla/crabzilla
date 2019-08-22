package io.github.crabzilla.pgc

import com.accounts.service.PgcDbProjectionsVerticle.Companion.amIAlreadyRunning
import io.reactiverse.pgclient.*
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.ServerSocket

val log: Logger = LoggerFactory.getLogger("Pgc")

fun writeModelPgPool(vertx: Vertx, config: JsonObject) : PgPool {
  val id = "WRITE"
  val writeOptions = PgPoolOptions()
    .setPort(config.getInteger("${id}_DATABASE_PORT"))
    .setHost(config.getString("${id}_DATABASE_HOST"))
    .setDatabase(config.getString("${id}_DATABASE_NAME"))
    .setUser(config.getString("${id}_DATABASE_USER"))
    .setPassword(config.getString("${id}_DATABASE_PASSWORD"))
    .setMaxSize(config.getInteger("${id}_DATABASE_POOL_MAX_SIZE"))
  return PgClient.pool(vertx, writeOptions)
}

fun readModelPgPool(vertx: Vertx, config: JsonObject) : PgPool {
  val id = "READ"
  val writeOptions = PgPoolOptions()
    .setPort(config.getInteger("${id}_DATABASE_PORT"))
    .setHost(config.getString("${id}_DATABASE_HOST"))
    .setDatabase(config.getString("${id}_DATABASE_NAME"))
    .setUser(config.getString("${id}_DATABASE_USER"))
    .setPassword(config.getString("${id}_DATABASE_PASSWORD"))
    .setMaxSize(config.getInteger("${id}_DATABASE_POOL_MAX_SIZE"))
  return PgClient.pool(vertx, writeOptions)
}

/**
 * https://dzone.com/articles/three-paradigms-of-asynchronous-programming-in-ver
 */
fun PgTransaction.runPreparedQuery(query: String, tuple: Tuple, future: Future<Void>) {
  this.preparedQuery(query, tuple) { ar2 ->
    //    println("running $query with $tuple")
    if (ar2.failed()) {
//      println("    failed ${ar2.cause()}" )
      future.fail(ar2.cause())
    } else {
      future.complete()
    }
  }
}

fun getConfig(vertx: Vertx, configFile: String) : Future<JsonObject> {
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
  vertx.eventBus().send<String>(amIAlreadyRunning(pingEndpoint), processId) { isWorking ->
    if (isWorking.succeeded()) {
      log.info("No need to start $verticle: " + isWorking.result().body())
    } else {
      log.info("*** Deploying $verticle")
      vertx.deployVerticle(verticle, dOpt) { wasDeployed ->
        if (wasDeployed.succeeded()) {
          log.info("$verticle started")
          future.complete(wasDeployed.result())
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
