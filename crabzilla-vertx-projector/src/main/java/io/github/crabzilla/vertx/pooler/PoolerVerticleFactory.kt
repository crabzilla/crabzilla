package io.github.crabzilla.vertx.pooler

import io.vertx.core.Verticle
import io.vertx.core.spi.VerticleFactory

class PoolerVerticleFactory(val poolerVerticles: Set<PoolerVerticle>) : VerticleFactory {

  override fun prefix(): String {
    return "crabzilla-pooler"
  }

  @Throws(Exception::class)
  override fun createVerticle(name: String, classLoader: ClassLoader): Verticle? {
    return poolerVerticles.associateBy({it.name}, {it})[name]
  }

}
