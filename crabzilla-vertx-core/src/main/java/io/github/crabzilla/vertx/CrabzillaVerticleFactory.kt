package io.github.crabzilla.vertx

import io.vertx.core.Verticle
import io.vertx.core.spi.VerticleFactory

class CrabzillaVerticleFactory(verticles: Set<CrabzillaVerticle>,
                               private val prefix: String) : VerticleFactory {

  private val map = verticles.associateBy({it.name}, {it})

  override fun prefix(): String {
    return prefix
  }

  @Throws(Exception::class)
  override fun createVerticle(name: String, classLoader: ClassLoader): Verticle? {
    return map[name.removePrefix(prefix() + ":")]
  }

}
