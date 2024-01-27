package io.github.crabzilla.command

import io.github.crabzilla.example1.customer.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterAndActivateCustomer
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
class PersistingCommandsT : AbstractCommandIT() {
  @Test
  fun `it can persist 1 command`(
    tc: VertxTestContext,
    vertx: Vertx,
  ) {
    val id = UUID.randomUUID().toString()
    val cmd = RegisterAndActivateCustomer(id, "c1", "is needed")
    commandComponent.handle(id, cmd)
      .compose { testRepo.getAllCommands() }
      .compose { commands -> testRepo.scanEvents(0L, 10).map { Pair(commands, it) } }
      .onFailure { tc.failNow(it) }
      .onSuccess { pair ->
        val (commands, events) = pair
        tc.verify {
          assertThat(commands.size).isEqualTo(1)
          assertThat(events.size).isEqualTo(2)
          val rowAsJson = commands.first()
          assertThat(rowAsJson.getString("causation_id")).isNull()
          assertThat(rowAsJson.getString("correlation_id")).isNull()
          val cmdAsJsonFroDb = rowAsJson.getJsonObject("cmd_payload")
          assertThat(cmdAsJsonFroDb.getString("type")).isEqualTo("RegisterAndActivateCustomer")
        }
        tc.completeNow()
      }
  }

  @Test
  fun `it can persist 2 commands`(
    tc: VertxTestContext,
    vertx: Vertx,
  ) {
    val id = UUID.randomUUID().toString()
    val cmd1 = RegisterAndActivateCustomer(id, "customer#1", "is needed")
    val cmd2 = DeactivateCustomer("it's not needed anymore")

    commandComponent.handle(id, cmd1)
      .compose { commandComponent.handle(id, cmd2) }
      .compose { testRepo.getAllCommands() }
      .compose { commands -> testRepo.scanEvents(0L, 10).map { Pair(commands, it) } }
      .onFailure { tc.failNow(it) }
      .onSuccess { pair ->
        val (commands, events) = pair
        println("--- Events")
        for (c in events) {
          println(c.encodePrettily())
        }
        println("--- Commands")
        for (c in commands) {
          println(c.encodePrettily())
        }
        tc.verify {
          assertThat(commands.size).isEqualTo(2)
          assertThat(events.size).isEqualTo(3)

          val rowAsJson1 = commands.first()
          assertThat(rowAsJson1.getString("causation_id")).isNull()
          assertThat(rowAsJson1.getString("correlation_id")).isNull()
          val cmdAsJsonFroDb1 = rowAsJson1.getJsonObject("cmd_payload")
          assertThat(cmdAsJsonFroDb1.getString("type")).isEqualTo("RegisterAndActivateCustomer")

          val rowAsJson2 = commands[1]
          assertThat(rowAsJson2.getString("causation_id")).isEqualTo(events[1].getString("id"))
          assertThat(rowAsJson2.getString("correlation_id")).isEqualTo(events[1].getString("correlation_id"))
          val cmdAsJsonFroDb2 = rowAsJson2.getJsonObject("cmd_payload")
          assertThat(cmdAsJsonFroDb2.getString("type")).isEqualTo("DeactivateCustomer")
          tc.completeNow()
        }
      }
  }
}
