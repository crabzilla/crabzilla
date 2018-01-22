package io.github.crabzilla.vertx.projection

import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.core.spi.VerticleFactory

class ProjectorVerticleFactory(val projectorVerticles: Set<ProjectionHandlerVerticle<out Any>>) : VerticleFactory {

  override fun prefix(): String {
    return "crabzilla-projector"
  }

  @Throws(Exception::class)
  override fun createVerticle(s: String, classLoader: ClassLoader): Verticle? {
    return projectorVerticles.first() // TODO match by name
  }

  override fun init(vertx: Vertx) {
  }

}
