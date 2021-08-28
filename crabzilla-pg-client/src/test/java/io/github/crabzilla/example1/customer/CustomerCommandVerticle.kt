package io.github.crabzilla.example1.customer

import io.github.crabzilla.core.serder.KotlinSerDer
import io.github.crabzilla.example1.example1Json
import io.github.crabzilla.pgc.PgcAbstractVerticle
import io.github.crabzilla.pgc.command.CommandController
import io.github.crabzilla.stack.command.CommandMetadata
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

// TODO a singleton command verticle could skip the optimistic locking but does it really worth?
class CustomerCommandVerticle : PgcAbstractVerticle() {

  companion object {
    private val log = LoggerFactory.getLogger(CustomerCommandVerticle::class.java)
    const val ENDPOINT = "customer.command.handler"
  }

  override fun start() {

    val pgPool = pgPool(config())

    val serDer = KotlinSerDer(example1Json)
    val eventStore = CommandController(vertx, customerConfig, pgPool, serDer, false)

    vertx.eventBus().consumer<JsonObject>(ENDPOINT) { msg ->
      val metadata = CommandMetadata.fromJson(msg.body().getJsonObject("metadata"))
      val command = serDer.commandFromJson(msg.body().getJsonObject("command").toString()) as CustomerCommand
      eventStore.handle(metadata, command)
        .onFailure { msg.fail(500, it.message) }
        .onSuccess { msg.reply(true) }
    }

    log.info("Successfully started")
  }
}
