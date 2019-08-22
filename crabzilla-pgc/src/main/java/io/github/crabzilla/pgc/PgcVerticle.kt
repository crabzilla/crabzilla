package com.accounts.service

import io.vertx.core.AbstractVerticle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory

abstract class PgcVerticle : AbstractVerticle() {

  companion object {
    val log: Logger = LoggerFactory.getLogger(PgcVerticle::class.java)
    val processId = ManagementFactory.getRuntimeMXBean().name // TODO does this work with AOT?
  }

  val projectionEndpoint: String by lazy {
    config().getString("PROJECTION_ENDPOINT")
  }

}
