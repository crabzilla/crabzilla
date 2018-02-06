package io.github.crabzilla.vertx

import io.vertx.core.AbstractVerticle

enum class VerticleRole(val prefix: String) {
  REST("rest"), HANDLER("handler"), PROJECTOR("projector"), POOLER("pooler") ;
  fun verticle(name: String): String {
    return prefix + ":" + name
  }
}

abstract class CrabzillaVerticle(open val name: String, val role: VerticleRole) : AbstractVerticle()
