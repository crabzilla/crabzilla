package io.github.crabzilla.spi

import io.vertx.core.AbstractVerticle

class TestVerticle : AbstractVerticle() {

//  lateinit var sqlClient:
  override fun start() {

//    val json = Class.forName("io.github.crabzilla.example1.JsonFactory")
//
//    print(json)

    println(config().encodePrettily())
  }
}
