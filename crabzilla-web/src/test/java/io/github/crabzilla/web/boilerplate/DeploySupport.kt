package io.github.crabzilla.web.boilerplate

import io.vertx.core.AsyncResult
import io.vertx.core.CompositeFuture
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.Vertx
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object DeploySupport {

  private val log: Logger = LoggerFactory.getLogger(DeploySupport::class.java)

  fun deploy(vertx: Vertx, verticle: String, deploymentOptions: DeploymentOptions): Future<String> {
    val promise = Promise.promise<String>()
    vertx.deployVerticle(verticle, deploymentOptions, promise)
    return promise.future()
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
}
