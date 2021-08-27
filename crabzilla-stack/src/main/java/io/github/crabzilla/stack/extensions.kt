package io.github.crabzilla.stack

import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Vertx

/**
 * Deploy verticles
 */
fun Vertx.deployVerticles(verticles: List<String>, opt: DeploymentOptions): Future<Void> {
  val initialFuture = Future.succeededFuture<String>()
  return verticles.fold(
    initialFuture
  ) { currentFuture: Future<String>, verticle: String ->
    currentFuture.compose {
      deployVerticle(verticle, opt)
    }
  }.mapEmpty()
}
