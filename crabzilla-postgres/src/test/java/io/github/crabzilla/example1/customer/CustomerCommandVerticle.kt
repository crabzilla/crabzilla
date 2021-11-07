package io.github.crabzilla.example1.customer

import io.github.crabzilla.postgres.PostgresAbstractVerticle
import io.github.crabzilla.postgres.command.CommandController
import io.github.crabzilla.postgres.command.CommandMetadataExt.fromJson
import io.github.crabzilla.postgres.command.PersistentSnapshotRepo
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

// TODO a singleton command verticle could skip the optimistic locking but does it really worth?
class CustomerCommandVerticle : PostgresAbstractVerticle() {

  companion object {
    private val log = LoggerFactory.getLogger(CustomerCommandVerticle::class.java)
    const val ENDPOINT = "customer.command.handler"
  }

  override fun start() {

    val snapshotRepo = PersistentSnapshotRepo<Customer, CustomerEvent>(customerConfig.name, jsonSerDer)
    val eventStore = CommandController(vertx, pgPool, jsonSerDer, customerConfig, snapshotRepo)

    vertx.eventBus().consumer<JsonObject>(ENDPOINT) { msg ->
      val metadata = fromJson(msg.body().getJsonObject("metadata"))
      val command = jsonSerDer.commandFromJson(msg.body().getJsonObject("command").toString()) as CustomerCommand
      eventStore.handle(metadata, command)
        .onFailure { msg.fail(500, it.message) }
        .onSuccess { msg.reply(true) }
    }

    log.info("Successfully started")
  }
}
