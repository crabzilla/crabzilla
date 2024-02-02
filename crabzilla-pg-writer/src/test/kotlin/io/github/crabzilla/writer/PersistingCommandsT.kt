package io.github.crabzilla.writer

import io.github.crabzilla.context.TargetStream
import io.github.crabzilla.example1.customer.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.customer.customerConfig
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
class PersistingCommandsT : AbstractCrabzillaWriterIT() {
  @Test
  fun `it can persist 1 command`(
    tc: VertxTestContext,
    vertx: Vertx,
  ) {
    val customerId1 = UUID.randomUUID()
    val targetStream = TargetStream(stateType = "Customer", stateId = customerId1.toString())
    val cmd = RegisterAndActivateCustomer(customerId1, "c1", "is needed")
    crabzillaWriter.handle(targetStream, cmd)
      .compose { testRepository.getCommands() }
      .onFailure { tc.failNow(it) }
      .onSuccess { commands ->
        tc.verify {
          assertThat(commands.size).isEqualTo(1)
          val command = commands.first()
          assertThat(command.getString("command_id")).isNotNull()
          assertThat(command.getString("causation_id")).isNull()
          assertThat(command.getString("correlation_id")).isNull()
          val cmdPayload = command.getJsonObject("command_payload")
          assertThat(cmdPayload.getString("type")).isEqualTo("RegisterAndActivateCustomer")
          assertThat(command.getJsonObject("command_metadata")).isNull()
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
    val cmdMetadata =
      CommandMetadata(commandId = UUID.randomUUID(), metadata = JsonObject().put("campaign", "black-friday-2024").put("user", "Peter"))
    crabzillaWriter.handle(targetStream, cmd, cmdMetadata)
      .compose { testRepository.getCommands() }
      .compose { commands -> testRepository.getEvents(0L, 10).map { Pair(commands, it) } }
      .onSuccess { pair ->
        val (commands, events) = pair
        tc.verify {
          assertThat(commands.size).isEqualTo(1)
          assertThat(events.size).isEqualTo(2)
          val rowAsJson = commands.first()
          assertThat(rowAsJson.getString("command_id")).isEqualTo(cmdMetadata.commandId)
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

    crabzillaWriter.handle(targetStream, cmd1)
      .compose { crabzillaWriter.handle(targetStream, cmd2) }
      .compose { testRepository.getCommands() }
      .compose { commands -> testRepository.getEvents(0L, 10).map { Pair(commands, it) } }
      .onFailure { tc.failNow(it) }
      .onSuccess { pair ->
        val (commands, events) = pair
        tc.verify {
          assertThat(commands.size).isEqualTo(2)
          assertThat(events.size).isEqualTo(3)

          val rowAsJson1 = commands.first()
          assertThat(rowAsJson1.getString("command_id")).isNotNull()
          assertThat(rowAsJson1.getString("causation_id")).isNull()
          assertThat(rowAsJson1.getString("correlation_id")).isNull()
          val cmdAsJsonFroDb1 = rowAsJson1.getJsonObject("command_payload")
          assertThat(cmdAsJsonFroDb1.getString("type")).isEqualTo("RegisterAndActivateCustomer")

          val rowAsJson2 = commands[1]
          assertThat(rowAsJson2.getString("command_id")).isNotNull()
          assertThat(rowAsJson2.getString("causation_id")).isEqualTo(events[1].getString("id"))
          assertThat(rowAsJson2.getString("correlation_id")).isEqualTo(events[1].getString("correlation_id"))
          val cmdAsJsonFroDb2 = rowAsJson2.getJsonObject("command_payload")
          assertThat(cmdAsJsonFroDb2.getString("type")).isEqualTo("DeactivateCustomer")
          tc.completeNow()
        }
      }
  }

  @Test
  fun `it can persist 1 command without body`(
    tc: VertxTestContext,
    vertx: Vertx,
  ) {
    // notice we removed the commandSerDer
    crabzillaWriter = CrabzillaWriterImpl(context, customerConfig.copy(commandSerDer = null))

    val customerId1 = UUID.randomUUID()
    val targetStream = TargetStream(stateType = "Customer", stateId = customerId1.toString())
    val cmd = RegisterAndActivateCustomer(customerId1, "c1", "is needed")

    crabzillaWriter.handle(targetStream, cmd)
      .compose { testRepository.getCommands() }
      .compose { commands -> testRepository.getEvents(0L, 10).map { Pair(commands, it) } }
      .onFailure { tc.failNow(it) }
      .onSuccess { pair ->
        val (commands, events) = pair
        tc.verify {
          assertThat(commands.size).isEqualTo(1)
          assertThat(events.size).isEqualTo(2)
          val command = commands.first()
          assertThat(command.getString("command_id")).isNotNull()
          assertThat(command.getString("causation_id")).isNull()
          assertThat(command.getString("correlation_id")).isNull()
          val commandPayload = command.getJsonObject("command_payload", null)
          assertThat(commandPayload).isNull()
          assertThat(command.getJsonObject("command_metadata")).isNull()
        }
        tc.completeNow()
      }
  }
}
