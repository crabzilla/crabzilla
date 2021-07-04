package io.github.crabzilla.example1

import io.github.crabzilla.core.Command
import io.github.crabzilla.pgc.PgcAbstractVerticle
import io.github.crabzilla.pgc.command.PgcEventStore
import io.github.crabzilla.pgc.command.PgcSnapshotRepo
import io.github.crabzilla.stack.CommandController
import io.github.crabzilla.stack.CommandMetadata
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
    val sqlClient = sqlClient(config())

    val snapshotRepo = PgcSnapshotRepo<Customer>(sqlClient, customerJson)
    val eventStore = PgcEventStore(customerConfig, pgPool, customerJson, false)
    val controller = CommandController(customerConfig, snapshotRepo, eventStore)

    vertx.eventBus().consumer<JsonObject>(ENDPOINT) { msg ->
      val metadata = CommandMetadata.fromJson(msg.body().getJsonObject("metadata"))
      val command = Command.fromJson<CustomerCommand>(customerJson, msg.body().getJsonObject("command").toString())
      controller.handle(metadata, command)
        .onFailure { msg.fail(500, it.message) }
        .onSuccess { msg.reply(true) }
    }

    log.info("Successfully started")
  }
}
