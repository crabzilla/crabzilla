package io.github.crabzilla

import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.Vertx
import org.slf4j.LoggerFactory

fun main() {

  val vertx = Vertx.vertx()

  vertx.deployVerticle(TestVerticle())
}

class TestVerticle : AbstractVerticle() {

  companion object {
    val log = LoggerFactory.getLogger(TestVerticle::class.simpleName)
  }

  val action = handler()

  override fun start() {
    vertx.setTimer(1000, action)
  }

  fun handler(): Handler<Long?> {
    return Handler { tick ->
      log.info("tick $tick")
      Thread.sleep(5000)
      vertx.setTimer(1000, action)
    }
  }
}
