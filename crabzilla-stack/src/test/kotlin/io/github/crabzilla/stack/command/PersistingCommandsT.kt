package io.github.crabzilla.stack.command

import io.github.crabzilla.TestsFixtures.jsonSerDer
import io.github.crabzilla.example1.customer.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.customer.customerConfig
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Persisting commands")
class PersistingCommandsT: AbstractCommandIT() {

  @Test
  fun `it can persist 1 command`(tc: VertxTestContext, vertx: Vertx) {
    val service = factory.commandService(customerConfig, jsonSerDer)
    val id = UUID.randomUUID().toString()
    val cmd = RegisterAndActivateCustomer(id, "c1", "is needed")
    service.handle(id, cmd)
      .compose { testRepo.getAllCommands() }
      .compose { commands -> testRepo.scanEvents(0L, 10).map { Pair(commands, it) } }
      .onFailure { tc.failNow(it) }
      .onSuccess { pair ->
        val (commands, events) = pair
        tc.verify {
          assertThat(commands.size).isEqualTo(1)
          assertThat(events.size).isEqualTo(2)
          val rowAsJson = commands.first()
          assertThat(rowAsJson.getString("causation_id")).isEqualTo(events.first().getString("id"))
          assertThat(rowAsJson.getString("last_causation_id")).isEqualTo(events.last().getString("id"))
          val cmdAsJsonFroDb = rowAsJson.getJsonObject("cmd_payload")
          assertThat(cmdAsJsonFroDb.getString("type")).isEqualTo("RegisterAndActivateCustomer")
        }
        tc.completeNow()
      }
  }

  @Test
  fun `it can persist 2 commands`(tc: VertxTestContext, vertx: Vertx) {

    val service = factory.commandService(customerConfig, jsonSerDer)

    val id = UUID.randomUUID().toString()
    val cmd1 = RegisterAndActivateCustomer(id, "customer#1", "is needed")
    val cmd2 = DeactivateCustomer("it's not needed anymore")

    service.handle(id, cmd1)
      .compose { service.handle(id, cmd2) }
      .compose { testRepo.getAllCommands() }
      .compose { commands -> testRepo.scanEvents(0L, 10).map { Pair(commands, it) } }
      .onFailure { tc.failNow(it) }
      .onSuccess { pair ->
        val (commands, events) = pair
        for (c in commands) {
          println(c.encodePrettily())
        }
        println("---")
        for (c in events) {
          println(c.encodePrettily())
        }
        tc.verify {
          assertThat(commands.size).isEqualTo(2)
          assertThat(events.size).isEqualTo(3)
          val rowAsJson1 = commands.first()
          assertThat(rowAsJson1.getString("causation_id")).isEqualTo(events.first().getString("id"))
          assertThat(rowAsJson1.getString("last_causation_id")).isEqualTo(events[1].getString("id"))
          val cmdAsJsonFroDb1 = rowAsJson1.getJsonObject("cmd_payload")
          assertThat(cmdAsJsonFroDb1.getString("type")).isEqualTo("RegisterAndActivateCustomer")
          val rowAsJson2 = commands[1]
          assertThat(rowAsJson2.getString("causation_id")).isEqualTo(events[2].getString("id"))
          assertThat(rowAsJson2.getString("last_causation_id")).isEqualTo(events.last().getString("id"))
          val cmdAsJsonFroDb2 = rowAsJson2.getJsonObject("cmd_payload")
          assertThat(cmdAsJsonFroDb2.getString("type")).isEqualTo("DeactivateCustomer")
          tc.completeNow()
        }
      }
  }
}
