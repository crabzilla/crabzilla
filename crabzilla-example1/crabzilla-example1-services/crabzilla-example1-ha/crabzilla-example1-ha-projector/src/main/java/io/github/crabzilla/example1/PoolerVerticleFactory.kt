package io.github.crabzilla.example1

import io.vertx.core.Verticle
import io.vertx.core.spi.VerticleFactory

class PoolerVerticleFactory(val poolerVerticles: Set<PoolerVerticle>) : VerticleFactory {

  override fun prefix(): String {
    return "crabzilla-pooler"
  }

  @Throws(Exception::class)
  override fun createVerticle(s: String, classLoader: ClassLoader): Verticle? {
    return poolerVerticles.first() // TODO match by name
  }

}
