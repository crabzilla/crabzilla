package io.github.crabzilla.pgclient.projection

import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory

fun Vertx.deployProjector(config: JsonObject, serviceName: String): Future<Void> {
  // TODO use hz quorum?
  val node = ManagementFactory.getRuntimeMXBean().name
  val serviceConfig = JsonObject(config.toBuffer()) // to not mutate config
  val projectionName = serviceName.removePrefix("service:")
  val log = LoggerFactory.getLogger(projectionName)
  serviceConfig.put("projectionName", projectionName)
  val promise = Promise.promise<Void>()
  this
    .eventBus()
    .request<String>("crabzilla.projectors.$projectionName.ping", node) { resp ->
      if (resp.failed()) {
        val projectionOptions = DeploymentOptions().setConfig(serviceConfig).setHa(true).setInstances(1)
        this.deployVerticle(serviceName, projectionOptions)
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
