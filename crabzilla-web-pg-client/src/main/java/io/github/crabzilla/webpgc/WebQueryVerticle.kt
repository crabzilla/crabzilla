package io.github.crabzilla.webpgc

import io.github.crabzilla.framework.Entity
import io.github.crabzilla.framework.EntityJsonAware
import io.github.crabzilla.pgc.readModelPgPool
import io.vertx.core.AbstractVerticle
import io.vertx.pgclient.PgPool
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class WebQueryVerticle : AbstractVerticle() {

  companion object {
    val log: Logger = LoggerFactory.getLogger(WebQueryVerticle::class.java)
  }

  val readDb : PgPool by lazy { readModelPgPool(vertx, config()) }
  val jsonFunctions: MutableMap<String, EntityJsonAware<out Entity>> = mutableMapOf()
  val httpPort : Int by lazy { config().getInteger("READ_HTTP_PORT")}

  fun addEntityJsonAware(entityName: String, jsonAware: EntityJsonAware<out Entity>) {
    jsonFunctions[entityName] = jsonAware
  }

}
