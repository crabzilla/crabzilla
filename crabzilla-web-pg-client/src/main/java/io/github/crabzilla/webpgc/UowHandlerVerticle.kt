package io.github.crabzilla.webpgc

import io.vertx.core.AbstractVerticle
import java.lang.management.ManagementFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class UowHandlerVerticle : AbstractVerticle() {

  companion object {
    val log: Logger = LoggerFactory.getLogger(UowHandlerVerticle::class.java)
    val processId: String = ManagementFactory.getRuntimeMXBean().name // TODO does this work with AOT?
  }

  override fun start() {
    val implClazz = this::class.java.name
    vertx.eventBus().consumer<String>(implClazz) { msg ->
      if (log.isTraceEnabled) log.trace("received " + msg.body())
      msg.reply("$implClazz is already running here: $processId")
    }
  }
}
