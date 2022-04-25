package io.github.crabzilla.projection

import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory

fun Vertx.deployProjector(config: JsonObject, serviceName: String): Future<Void> {
  val node = ManagementFactory.getRuntimeMXBean().name
  val serviceConfig = JsonObject(config.toBuffer()) // to not mutate config
  val projectionName = serviceName.removePrefix("service:")
  val log = LoggerFactory.getLogger(projectionName)
  serviceConfig.put("projectionName", projectionName)
  val projectorEndpoints = ProjectorEndpoints(projectionName)
  val promise = Promise.promise<Void>()
  this
    .eventBus()
    .request<JsonObject>(projectorEndpoints.status(), node) { resp ->
      if (resp.failed()) {
        val projectionOptions = DeploymentOptions().setConfig(serviceConfig).setHa(true).setInstances(1)
        this.deployVerticle(serviceName, projectionOptions)
          .onSuccess {
            promise.complete()
            log.info("Started {}", serviceName)
          }
          .onFailure {
            promise.fail(it)
          }
      } else {
        promise.complete()
        log.info(
          "Started as standby since node ${resp.result().body().getString("node")} " +
            "is the current owner of this verticle"
        )
      }
    }
  return promise.future()
}
