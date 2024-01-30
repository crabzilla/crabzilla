package io.github.crabzilla.handler

import io.github.crabzilla.example1.customer.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterAndActivateCustomer
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
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
    val customerId1 = UUID.randomUUID()
    val targetStream = TargetStream(stateType = "Customer", stateId = customerId1.toString())
    val cmd = RegisterAndActivateCustomer(customerId1, "c1", "is needed")
    crabzillaHandler.handle(targetStream, cmd)
      .compose { testRepository.getCommands() }
      .compose { commands -> testRepository.scanEvents(0L, 10).map { Pair(commands, it) } }
      .onFailure { tc.failNow(it) }
      .onSuccess { pair ->
        val (commands, events) = pair
        println("--- Commands")
        for (c in commands) {
          println(c.encodePrettily())
        }
        println("--- Events")
        for (e in events) {
          println(e.encodePrettily())
        }
        tc.verify {
          assertThat(commands.size).isEqualTo(1)
          assertThat(events.size).isEqualTo(2)
          val rowAsJson = commands.first()
          assertThat(rowAsJson.getString("causation_id")).isNull()
          assertThat(rowAsJson.getString("correlation_id")).isNull()
          val cmdAsJsonFroDb = rowAsJson.getJsonObject("command_payload")
          assertThat(cmdAsJsonFroDb.getString("type")).isEqualTo("RegisterAndActivateCustomer")
          assertThat(rowAsJson.getJsonObject("command_metadata")).isNull()
        }
        tc.completeNow()
      }
  }

  @Test
  fun `it can persist 1 command with metadata`(
    tc: VertxTestContext,
    vertx: Vertx,
  ) {
    val customerId1 = UUID.randomUUID()
    val targetStream = TargetStream(stateType = "Customer", stateId = customerId1.toString())
    val cmd = RegisterAndActivateCustomer(customerId1, "c1", "is needed")
    val cmdMetadata = CommandMetadata(metadata = JsonObject().put("campaign", "black-friday-2024").put("user", "Peter"))
    crabzillaHandler.handle(targetStream, cmd, cmdMetadata)
      .compose { testRepository.getCommands() }
      .compose { commands -> testRepository.scanEvents(0L, 10).map { Pair(commands, it) } }
      .onSuccess { pair ->
        val (commands, events) = pair
        println("--- Commands")
        for (c in commands) {
          println(c.encodePrettily())
        }
        println("--- Events")
        for (c in events) {
          println(c.encodePrettily())
        }
        tc.verify {
          assertThat(commands.size).isEqualTo(1)
          assertThat(events.size).isEqualTo(2)
          val rowAsJson = commands.first()
          assertThat(rowAsJson.getString("causation_id")).isNull()
          assertThat(rowAsJson.getString("correlation_id")).isNull()
          val cmdAsJsonFroDb = rowAsJson.getJsonObject("command_payload")
          assertThat(cmdAsJsonFroDb.getString("type")).isEqualTo("RegisterAndActivateCustomer")
          assertThat(rowAsJson.getJsonObject("command_metadata")).isEqualTo(cmdMetadata.metadata)
        }
        tc.completeNow()
      }
  }

  @Test
  fun `it can persist 2 commands`(
    tc: VertxTestContext,
    vertx: Vertx,
  ) {
    val customerId1 = UUID.randomUUID()
    val targetStream = TargetStream(stateType = "Customer", stateId = customerId1.toString())
    val cmd1 = RegisterAndActivateCustomer(customerId1, "customer#2", "is needed")
    val cmd2 = DeactivateCustomer("it's not needed anymore")

    crabzillaHandler.handle(targetStream, cmd1)
      .compose { crabzillaHandler.handle(targetStream, cmd2) }
      .compose { testRepository.getCommands() }
      .compose { commands -> testRepository.scanEvents(0L, 10).map { Pair(commands, it) } }
      .onFailure { tc.failNow(it) }
      .onSuccess { pair ->
        val (commands, events) = pair
        println("--- Commands")
        for (c in commands) {
          println(c.encodePrettily())
        }
        println("--- Events")
        for (c in events) {
          println(c.encodePrettily())
        }
        tc.verify {
          assertThat(commands.size).isEqualTo(2)
          assertThat(events.size).isEqualTo(3)

          val rowAsJson1 = commands.first()
          assertThat(rowAsJson1.getString("causation_id")).isNull()
          assertThat(rowAsJson1.getString("correlation_id")).isNull()
          val cmdAsJsonFroDb1 = rowAsJson1.getJsonObject("command_payload")
          assertThat(cmdAsJsonFroDb1.getString("type")).isEqualTo("RegisterAndActivateCustomer")

          val rowAsJson2 = commands[1]
          assertThat(rowAsJson2.getString("causation_id")).isEqualTo(events[1].getString("id"))
          assertThat(rowAsJson2.getString("correlation_id")).isEqualTo(events[1].getString("correlation_id"))
          val cmdAsJsonFroDb2 = rowAsJson2.getJsonObject("command_payload")
          assertThat(cmdAsJsonFroDb2.getString("type")).isEqualTo("DeactivateCustomer")
          tc.completeNow()
        }
      }
  }
}
