package io.github.crabzilla.vertx.entity

import io.github.crabzilla.core.entity.Entity
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.core.spi.VerticleFactory

class CmdHandlerVerticleFactory(val verticles: Set<EntityCommandHandlerVerticle<out Entity>>) : VerticleFactory {

  override fun prefix(): String {
    return "crabzilla-command-handler"
  }

  @Throws(Exception::class)
  override fun createVerticle(s: String, classLoader: ClassLoader): Verticle? {
    return verticles.first() // TODO match by name
  }

  override fun init(vertx: Vertx) {
  }

}
