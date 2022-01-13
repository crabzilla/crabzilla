package io.github.crabzilla.accounts

import io.github.crabzilla.pgclient.PgClientAbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory

// TODO move this to crabzilla
fun <V: PgClientAbstractVerticle> Vertx.deployProcessor(config: JsonObject, clazz: Class<V>): Future<Void> {
  // TODO use hz quorum?
  val node = ManagementFactory.getRuntimeMXBean().name
  val log = LoggerFactory.getLogger(clazz)
  val promise = Promise.promise<Void>()
  log.info("Listening to crabzilla.${clazz.canonicalName}.ping")
  this
    .eventBus()
    .request<String>("crabzilla.${clazz.canonicalName}.ping", node) { resp ->
      if (resp.failed()) {
        val projectionOptions = DeploymentOptions().setConfig(config).setHa(true).setInstances(1)
        this.deployVerticle(clazz.canonicalName, projectionOptions)
          .onSuccess {
            promise.complete()
            log.info("Started {}", clazz.canonicalName)
          }
          .onFailure {
            promise.fail(it)
            log.error("When starting {}", clazz.canonicalName, it)
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
