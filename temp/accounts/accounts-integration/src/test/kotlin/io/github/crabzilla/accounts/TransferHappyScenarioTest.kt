package io.github.crabzilla.accounts

import io.github.crabzilla.accounts.Helpers.asJson
import io.github.crabzilla.accounts.domain.accounts.AccountCommand
import io.github.crabzilla.accounts.domain.transfers.TransferCommand
import io.github.crabzilla.accounts.processors.AccountOpenedProjector
import io.github.crabzilla.accounts.processors.PendingTransfersVerticle
import io.github.crabzilla.core.metadata.CommandMetadata
import io.github.crabzilla.json.KotlinJsonSerDer
import io.github.crabzilla.pgclient.command.CommandsContext
import io.github.crabzilla.pgclient.command.SnapshotType
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.pgclient.PgPool
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import java.util.UUID

@ExtendWith(VertxExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class TransferHappyScenarioTest {

  companion object {

    private val log = LoggerFactory.getLogger(TransferHappyScenarioTest::class.java)

    // create acct1
    val md1 = CommandMetadata(UUID.randomUUID())
    val cmd1 = AccountCommand.OpenAccount(md1.stateId, "cpf1", "person1")

    // deposit 100 on acct1
    val md11 = CommandMetadata(md1.stateId)
    val cmd11 = AccountCommand.DepositMoney(100.00)

    // create acct2
    val md2 = CommandMetadata(UUID.randomUUID())
    val cmd2 = AccountCommand.OpenAccount(md2.stateId, "cpf2", "person2")

    // transfer from acct1 to acct2
    val md3 = CommandMetadata(UUID.randomUUID())
    val cmd3 = TransferCommand.RequestTransfer(md3.stateId, 60.00,
      fromAccountId = md1.stateId, toAccountId = md2.stateId)

    val accountsSerDer = KotlinJsonSerDer(PendingTransfersVerticle.accountJson)
    val transfersSerDer = KotlinJsonSerDer(PendingTransfersVerticle.transferJson)

    private const val DEFAULT_WAIT_MS = 2000L

    fun configRetriever(vertx: Vertx): ConfigRetriever {
      val fileStore = ConfigStoreOptions()
        .setType("file")
        .setConfig(JsonObject().put("path", "./../conf/config.test.json"))
      val options = ConfigRetrieverOptions().addStore(fileStore)
      return ConfigRetriever.create(vertx, options)
    }

    @BeforeAll
    @JvmStatic
    fun deployMainVerticle(vertx: Vertx, testContext: VertxTestContext) {
      configRetriever(vertx).config
        .onSuccess { config ->
          vertx.deployVerticle(MainVerticle(), DeploymentOptions().setConfig(config), testContext.succeeding {
            testContext.completeNow()
          }
          )
        }
    }

  }

  private fun cleanDatabase(pgPool: PgPool): Future<Void> {
    return pgPool
      .query("delete from commands").execute()
      .compose { pgPool.query("delete from events").execute() }
      .compose { pgPool.query("delete from snapshots").execute() }
      .compose { pgPool.query("delete from accounts_view").execute() }
      .compose { pgPool.query("delete from transfers_view").execute() }
      .compose { pgPool.query("update projections set sequence = 0").execute() }
      .mapEmpty()
  }

  @Test
  @Order(1)
  fun `given a fresh database and an account A with 100 and B with 0`(vertx: Vertx, tc: VertxTestContext) {
    configRetriever(vertx).config
      .compose { config ->
        val acctContext = CommandsContext.create(vertx, accountsSerDer,
          config.getJsonObject("accounts-db-config"))
        val acctController = acctContext.create(PendingTransfersVerticle.accountConfig, SnapshotType.ON_DEMAND,
          AccountOpenedProjector("accounts_view"))
        cleanDatabase(acctContext.pgPool)
          .compose {
            log.info("Will handle {}", cmd1)
            acctController.handle(md1, cmd1)
              .compose {
                log.info("Will handle {}", cmd11)
                acctController.handle(md11, cmd11)
              }
              .compose {
                log.info("Will handle {}", cmd2)
                acctController.handle(md2, cmd2)
              }
          }
          .onSuccess {
            Thread.sleep(DEFAULT_WAIT_MS) // to give some time to background process
            tc.completeNow()
          }
          .onFailure { tc.failNow(it) }
      }
  }

  @Test
  @Order(2)
  fun `when transferring 60 from account A to B`(vertx: Vertx, tc: VertxTestContext) {
    configRetriever(vertx).config
      .compose { config ->
        val transferController = CommandsContext.create(vertx, transfersSerDer,
          config.getJsonObject("accounts-db-config"))
          .create(PendingTransfersVerticle.transferConfig, SnapshotType.ON_DEMAND)
        log.info("Will handle {}", cmd3)
        transferController.handle(md3, cmd3)
          .onSuccess {
            Thread.sleep(DEFAULT_WAIT_MS) // to give some time to background process
            tc.completeNow()
          }
          .onFailure { tc.failNow(it) }
      }
  }

  @Test
  @Order(3)
  fun `then after triggering transfers processor`(vertx: Vertx, tc: VertxTestContext) {
    vertx.eventBus()
      .request<Void>("crabzilla.io.github.crabzilla.accounts.processors.PendingTransfersVerticle.ping",
        "go!") {
        Thread.sleep(DEFAULT_WAIT_MS) // to give some time to background process
        tc.completeNow()
      }
  }

  @Test
  @Order(4)
  fun `and after triggering accounts projector`(vertx: Vertx, tc: VertxTestContext) {
    vertx.eventBus().request<Void>("crabzilla.projectors.projectors.accounts.AccountsView", "go!") {
      Thread.sleep(DEFAULT_WAIT_MS) // to give some time to background process
      tc.completeNow()
    }
  }

  @Test
  @Order(5)
  fun `and after triggering transfers projector`(vertx: Vertx, tc: VertxTestContext) {
    vertx.eventBus().request<Void>("crabzilla.projectors.projectors.transfers.TransfersView", "go!") {
      Thread.sleep(DEFAULT_WAIT_MS) // to give some time to background process
      tc.completeNow()
    }
  }

  @Test
  @Order(6)
  fun `then the view model is up to date and consistent`(vertx: Vertx, tc: VertxTestContext) {
    Thread.sleep(DEFAULT_WAIT_MS) // to give some time to background process
    configRetriever(vertx).config
      .compose { config ->
        val commandsContext1 = KotlinJsonSerDer(PendingTransfersVerticle.accountJson)
          .let { CommandsContext.create(vertx, it, config.getJsonObject("accounts-db-config")) }
        commandsContext1.pgPool
          .query("select * from accounts_view")
          .execute()
          .compose {
            Future.succeededFuture(it.asJson("id"))
          }.compose { accounts ->
            commandsContext1.pgPool
              .query("select * from transfers_view")
              .execute()
              .map { Pair(accounts, it.asJson("id")) }
          }
          .onSuccess { pair ->
            val (accounts, transfers) = pair
            // accounts
            log.info("Accounts view {}", accounts.encodePrettily())
            val account1 = accounts.getJsonObject(md1.stateId.toString())
            if (account1.getDouble("balance") != 40.00) {
              tc.failNow("acct1 should have balance = 40.00")
            }
            val account2 = accounts.getJsonObject(md2.stateId.toString())
            if (account2.getDouble("balance") != 60.00) {
              tc.failNow("acct2 should have balance = 60.00")
            }
            // transfer
            log.info("Transfers view {}", transfers.encodePrettily())
            val transfer = transfers.getJsonObject(md3.stateId.toString())
            if (transfer.getDouble("amount") != 60.00) {
              tc.failNow("transfer should have amount = 100.00")
            }
            if (transfer.getBoolean("pending")) {
              tc.failNow("transfer should not be pending")
            }
            if (!transfer.getBoolean("succeeded")) {
              tc.failNow("transfer should be succeeded")
            }
            tc.completeNow()
          }
          .onFailure { tc.failNow(it) }
      }
  }

}
