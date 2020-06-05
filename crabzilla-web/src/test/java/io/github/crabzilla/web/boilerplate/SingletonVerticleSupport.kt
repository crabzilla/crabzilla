package io.github.crabzilla.web.boilerplate

import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory

object SingletonVerticleSupport {

  private val log: Logger = LoggerFactory.getLogger(SingletonVerticleSupport::class.java)

  interface SingletonClusteredVerticle : Verticle

  fun deploySingleton(vertx: Vertx, verticle: SingletonClusteredVerticle, dOpt: DeploymentOptions, processId: String):
          Future<String> {

    val promise = Promise.promise<String>()
    val verticleClassName = verticle::class.java.name
    vertx.eventBus().request<JsonObject>(verticleClassName, processId) { gotResponse ->
      if (gotResponse.succeeded()) {
        log.info("No need to deploy $verticle: " + gotResponse.result().body().encodePrettily())
      } else {
        log.info("*** Deploying $verticle")
        vertx.deployVerticle(verticle, dOpt) { wasDeployed ->
          if (wasDeployed.succeeded()) {
            log.info("$verticle started")
            promise.complete("singleton ${wasDeployed.result()}")
          } else {
            log.error("$verticle not started", wasDeployed.cause())
            promise.fail(wasDeployed.cause())
          }
        }
      }
    }
    return promise.future()
  }

  fun addSingletonListener(vertx: Vertx, verticle: SingletonClusteredVerticle) {
    val processId: String = ManagementFactory.getRuntimeMXBean().name
    val verticleClassName = verticle::class.java.name
    // this is only a boilerplate to make sure this verticle is already deployed within a clustered
    vertx.eventBus().consumer<String>(verticleClassName) { areYouThereRequest ->
      if (log.isDebugEnabled) log.debug("$verticleClassName received " + areYouThereRequest.body())
      val response = jsonObjectOf(Pair("class", verticleClassName),
              Pair("processId", processId), Pair("deploymentId", vertx.orCreateContext.deploymentID()))
      areYouThereRequest.reply(response)
    }
  }
}
