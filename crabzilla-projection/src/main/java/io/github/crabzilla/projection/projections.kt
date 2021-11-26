package io.github.crabzilla.projection

import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.slf4j.Logger
import java.lang.management.ManagementFactory

fun deployProjector(
  log: Logger,
  vertx: Vertx,
  config: JsonObject,
  serviceName: String,
  isClustered: Boolean = false,
)
        : Future<Void> {
  // TODO use hz quorum?
  val node = ManagementFactory.getRuntimeMXBean().name
  val serviceConfig = JsonObject(config.toBuffer()) // to not mutate config
  val projectionName = serviceName.removePrefix("service:")
  serviceConfig.put("projectionName", projectionName)
  val promise = Promise.promise<Void>()
  vertx
    .eventBus()
    .request<String>("crabzilla.projectors.$projectionName.ping", node) { resp ->
      log.info("Got response from verticle {}", serviceName)
      if (resp.failed()) {
        val projectionOptions = DeploymentOptions().setConfig(serviceConfig).setHa(isClustered).setInstances(1)
        vertx.deployVerticle(serviceName, projectionOptions)
          .onSuccess {
            promise.complete()
            log.info("Started {}", serviceName)
          }
          .onFailure {
            promise.fail(it)
            log.error("When starting {}", serviceName, it)
          }
      } else {
        promise.complete()
        log.info(
          "Started as standby since node ${resp.result().body()} " +
                  "is the current owner of this verticle"
        )
      }
    }
  return promise.future()
}
