package io.github.crabzilla.kotlinx

import io.github.crabzilla.TestRepository
import io.github.crabzilla.cleanDatabase
import io.github.crabzilla.example1.customer.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterAndActivateCustomer
import io.github.crabzilla.example1.customer.customerComponent
import io.github.crabzilla.example1.customer.customerModule
import io.github.crabzilla.stack.CrabzillaVertxContext
import io.github.crabzilla.testDbConfig
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Persisting commands")
class PersistingCommandsT {

  private lateinit var context : CrabzillaVertxContext
  private lateinit var testRepo: TestRepository

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    context = CrabzillaVertxContext.new(vertx, testDbConfig)
    testRepo = TestRepository(context.pgPool())
    cleanDatabase(context.pgPool())
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @Test
  fun `it can persist 1 command`(tc: VertxTestContext, vertx: Vertx) {
    val service = context.kotlinxCommandService(customerComponent, customerModule)
    val id = UUID.randomUUID()
    val cmd = RegisterAndActivateCustomer(id, "c1", "is needed")
    service.handle(id, cmd)
      .onFailure { tc.failNow(it) }
      .onSuccess { appendedEvents ->
        testRepo.getAllCommands()
          .onFailure { tc.failNow(it) }
          .onSuccess { list ->
            tc.verify {
              assertThat(list.size).isEqualTo(1)
              val rowAsJson = list.first()
              assertThat(UUID.fromString(rowAsJson.getString("causation_id")))
                .isEqualTo(appendedEvents.first().metadata.causationId)
              val cmdAsJsonFroDb = rowAsJson.getJsonObject("cmd_payload")
              assertThat(cmdAsJsonFroDb.getString("type")).isEqualTo("RegisterAndActivateCustomer")
            }
            tc.completeNow()
          }
      }
  }

  @Test
  fun `it can persist 2 commands`(tc: VertxTestContext, vertx: Vertx) {

    val service = context.kotlinxCommandService(customerComponent, customerModule)

    val id = UUID.randomUUID()
    val cmd1 = RegisterAndActivateCustomer(id, "customer#1", "is needed")
    val cmd2 = DeactivateCustomer("it's not needed anymore")

    service.handle(id, cmd1)
      .onFailure { tc.failNow(it) }
      .onSuccess {
        service.handle(id, cmd2)
          .onFailure { tc.failNow(it) }
          .onSuccess {
            testRepo.getAllCommands()
              .onFailure { tc.failNow(it) }
              .onSuccess { list ->
                tc.verify {
                  assertThat(list.size).isEqualTo(2)
                  val rowAsJson1 = list.first()
                  val cmdAsJsonFroDb1 = rowAsJson1.getJsonObject("cmd_payload")
                  assertThat(cmdAsJsonFroDb1.getString("type")).isEqualTo("RegisterAndActivateCustomer")
                  val rowAsJson2 = list[1]
                  val cmdAsJsonFroDb2 = rowAsJson2.getJsonObject("cmd_payload")
                  assertThat(cmdAsJsonFroDb2.getString("type")).isEqualTo("DeactivateCustomer")
                  tc.completeNow()
                }
              }
          }
      }
  }
}
