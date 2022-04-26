package io.github.crabzilla.command

import io.github.crabzilla.TestsFixtures.commandSerDer
import io.github.crabzilla.TestsFixtures.json
import io.github.crabzilla.TestsFixtures.pgPool
import io.github.crabzilla.TestsFixtures.testRepo
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.example1.customer.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.customer.customerComponent
import io.github.crabzilla.stack.CommandController
import io.github.crabzilla.stack.CommandMetadata
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Persisting commands")
class PersistingCommandsT {


  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    cleanDatabase(pgPool)
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  fun `it can persist 1 command`(tc: VertxTestContext, vertx: Vertx) {
    val repository = KotlinxCommandRepository(json, customerComponent)
    val controller = CommandController(vertx, pgPool, customerComponent, repository)

    val id = UUID.randomUUID()
    val cmd = RegisterAndActivateCustomer(id, "c1", "is needed")
    val metadata = CommandMetadata.new(id)
    controller.handle(metadata, cmd)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        testRepo.getAllCommands()
          .onFailure { tc.failNow(it) }
          .onSuccess { list ->
            assertThat(list.size).isEqualTo(1)
            val rowAsJson = list.first()
            assertThat(UUID.fromString(rowAsJson.getString("cmd_id"))).isEqualTo(metadata.commandId)
            val cmdAsJsonFroDb = rowAsJson.getJsonObject("cmd_payload")
            val cmdFromDb = json.decodeFromString(commandSerDer, cmdAsJsonFroDb.toString())
            assertThat(cmdFromDb).isEqualTo(cmd)
            tc.completeNow()
          }
      }
  }

  @Test
  fun `it can persist 2 commands`(tc: VertxTestContext, vertx: Vertx) {

    val repository = KotlinxCommandRepository(json, customerComponent)
    val controller = CommandController(vertx, pgPool, customerComponent, repository)

    val id = UUID.randomUUID()
    val cmd1 = RegisterAndActivateCustomer(id, "customer#1", "is needed")
    val metadata1 = CommandMetadata.new(id)
    val cmd2 = DeactivateCustomer("it's not needed anymore")
    val metadata2 = CommandMetadata.new(id)

    controller.handle(metadata1, cmd1)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        controller.handle(metadata2, cmd2)
          .onFailure { tc.failNow(it) }
          .onSuccess {
            testRepo.getAllCommands()
              .onFailure { tc.failNow(it) }
              .onSuccess { list ->
                assertThat(list.size).isEqualTo(2)

                val rowAsJson1 = list.first()
                assertThat(UUID.fromString(rowAsJson1.getString("cmd_id"))).isEqualTo(metadata1.commandId)
                val cmdAsJsonFroDb1 = rowAsJson1.getJsonObject("cmd_payload")
                val cmdFromDb1 = json.decodeFromString(commandSerDer, cmdAsJsonFroDb1.toString())
                assertThat(cmdFromDb1).isEqualTo(cmd1)

                val rowAsJson2 = list.last()
                assertThat(UUID.fromString(rowAsJson2.getString("cmd_id"))).isEqualTo(metadata2.commandId)
                val cmdAsJsonFroDb2 = rowAsJson2.getJsonObject("cmd_payload")
                val cmdFromDb2 = json.decodeFromString(commandSerDer, cmdAsJsonFroDb2.toString())
                assertThat(cmdFromDb2).isEqualTo(cmd2)

                tc.completeNow()
              }
          }
      }
  }
}
