package io.github.crabzilla.stack

import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Vertx
import java.util.function.BiFunction

/**
 * A helper
 */
fun <A, B> foldLeft(iterator: Iterator<A>, identity: B, bf: BiFunction<B, A, B>): B {
  var result = identity
  while (iterator.hasNext()) {
    val next = iterator.next()
    result = bf.apply(result, next)
  }
  return result
}

/**
 * Deploy verticles
 */
fun Vertx.deployVerticles(
  verticles: List<String>,
  opt: DeploymentOptions = DeploymentOptions().setInstances(1)
): Future<Void> {
  val initialFuture = Future.succeededFuture<String>()
  return foldLeft(
    verticles.iterator(),
    initialFuture
  ) { currentFuture: Future<String>, verticle: String ->
    currentFuture.compose {
      deployVerticle(verticle, opt)
    }
  }.mapEmpty()
}
