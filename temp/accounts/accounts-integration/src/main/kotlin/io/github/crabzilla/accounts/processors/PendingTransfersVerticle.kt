package io.github.crabzilla.accounts.processors

import com.github.f4b6a3.uuid.UuidCreator
import io.github.crabzilla.accounts.domain.accounts.Account
import io.github.crabzilla.accounts.domain.accounts.AccountCommand
import io.github.crabzilla.accounts.domain.accounts.AccountCommandHandler
import io.github.crabzilla.accounts.domain.accounts.AccountEvent
import io.github.crabzilla.accounts.domain.accounts.AccountsSerialization
import io.github.crabzilla.accounts.domain.accounts.accountEventHandler
import io.github.crabzilla.accounts.domain.transfers.Transfer
import io.github.crabzilla.accounts.domain.transfers.TransferCommand
import io.github.crabzilla.accounts.domain.transfers.TransferCommand.RegisterResult
import io.github.crabzilla.accounts.domain.transfers.TransferCommandHandler
import io.github.crabzilla.accounts.domain.transfers.TransferEvent
import io.github.crabzilla.accounts.domain.transfers.TransfersSerialization
import io.github.crabzilla.accounts.domain.transfers.transferEventHandler
import io.github.crabzilla.core.command.CommandControllerConfig
import io.github.crabzilla.core.metadata.CommandMetadata
import io.github.crabzilla.json.KotlinJsonSerDer
import io.github.crabzilla.pgclient.PgClientAbstractVerticle
import io.github.crabzilla.pgclient.command.CommandController
import io.github.crabzilla.pgclient.command.CommandSideEffect
import io.github.crabzilla.pgclient.command.CommandsContext
import io.github.crabzilla.pgclient.command.SnapshotType
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory
import java.util.UUID

class PendingTransfersVerticle : PgClientAbstractVerticle() {

  companion object {
    val accountJson = Json { serializersModule = AccountsSerialization.accountModule }
    val accountConfig = CommandControllerConfig("Account", accountEventHandler, { AccountCommandHandler() })
    val transferJson = Json { serializersModule = TransfersSerialization.transferModule }
    val transferConfig = CommandControllerConfig("Transfer", transferEventHandler, { TransferCommandHandler() })
    private val log = LoggerFactory.getLogger(PendingTransfersVerticle::class.java)
    private val node = ManagementFactory.getRuntimeMXBean().name
    private const val DEFAULT_INTERVAL = 5_000L
  }


  override fun start() {

    config().put("connectOptionsName", "accounts-db-config")

    val acctController = KotlinJsonSerDer(accountJson)
      .let { CommandsContext.create(vertx, it, config().getJsonObject("accounts-db-config")) }
      .create(accountConfig, SnapshotType.ON_DEMAND, AccountOpenedProjector("accounts_view"))

    val transferController = KotlinJsonSerDer(transferJson)
      .let { CommandsContext.create(vertx, it, config().getJsonObject("accounts-db-config")) }
      .create(transferConfig, SnapshotType.ON_DEMAND)

    log.info("Starting with interval (ms) = {}", config().getLong("transfer.processor.interval", DEFAULT_INTERVAL))

    vertx.eventBus().consumer<String>("crabzilla." + this::class.java.name + ".ping") { msg ->
      log.info("Received a request to pull and process")
      pullAndProcess(acctController, transferController)
        .onComplete {
          if (it.succeeded()) {
            msg.reply(node)
          } else {
            msg.fail(500, it.cause().message)
          }
        }
    }

    vertx.setPeriodic(config().getLong("transfer.processor.interval", DEFAULT_INTERVAL)) {
      pullAndProcess(acctController, transferController)
    }

  }

  private data class PendingTransfer(
    val id: UUID, val amount: Double, val fromAccountId: UUID, val toAccountId: UUID,
    val causationId: UUID, val correlationId: UUID,
  )

  private fun pullAndProcess(
    acctController: CommandController<Account, AccountCommand, AccountEvent>,
    transferController: CommandController<Transfer, TransferCommand, TransferEvent>
  ): Future<Void> {
    return getPendingTransfers(pgPool)
      .compose { pendingList ->
        log.info("Found ${pendingList.size} pending transfers")
        val initialFuture = Future.succeededFuture<Void>()
        pendingList.fold(
          initialFuture
        ) { currentFuture: Future<Void>, pendingTransfer ->
          currentFuture.compose {
            handle(pendingTransfer, acctController, transferController)
          }
        }
      }
  }


  /**
   * Get 100 first pending transfers
   */
  private fun getPendingTransfers(pgPool: PgPool): Future<List<PendingTransfer>> {
    return pgPool.preparedQuery("select * from transfers_view where pending = true LIMIT 100")
      .execute()
      .map { rs: RowSet<Row> ->
        rs.iterator().asSequence().map { row ->
          PendingTransfer(row.getUUID("id"),
            row.getDouble("amount"),
            row.getUUID("from_acct_id"),
            row.getUUID("to_acct_id"),
            row.getUUID("causation_id"),
            row.getUUID("correlation_id")
          )
        }.toList()
      }
  }

  /**
   * Steps within the same db transaction:
   * fromAcctId withdrawn
   * toAcctId deposit
   * transferId register success
   * in case of error, the failure will be registered into a new db tx
   */
  private fun handle(
    pendingTransfer: PendingTransfer,
    acctController: CommandController<Account, AccountCommand, AccountEvent>,
    transferController: CommandController<Transfer, TransferCommand, TransferEvent>,
  ): Future<Void> {

    val promise = Promise.promise<Void>()
    val transferId = pendingTransfer.id
    val correlationId = pendingTransfer.correlationId

    acctController.compose { conn ->
      log.info("Step 1 - Will withdrawn from account {}", pendingTransfer.fromAccountId)
      val withdrawnMetadata = CommandMetadata(
        stateId = pendingTransfer.fromAccountId,
        commandId = UuidCreator.getTimeOrdered(),
        causationId = pendingTransfer.causationId,
        correlationId = correlationId)
      val withdrawnCommand = AccountCommand.WithdrawMoney(pendingTransfer.amount)
      acctController.handle(conn, withdrawnMetadata, withdrawnCommand)
        .compose { r1: CommandSideEffect ->
          log.info("Step 2 - Will deposit to account {}", pendingTransfer.toAccountId)
          val depositMetadata = CommandMetadata(
            stateId = pendingTransfer.toAccountId,
            commandId = UuidCreator.getTimeOrdered(),
            causationId = r1.appendedEvents.last().second.eventId,
            correlationId = correlationId)
          val depositCommand = AccountCommand.DepositMoney(pendingTransfer.amount)
          acctController.handle(conn, depositMetadata, depositCommand)
        }.compose { r2: CommandSideEffect ->
          log.info("Step 3 - Will register a succeeded transfer")
          val registerSuccessMetadata = CommandMetadata(
            stateId = transferId,
            commandId = UuidCreator.getTimeOrdered(),
            causationId = r2.appendedEvents.last().second.eventId,
            correlationId = correlationId)
          val registerSuccessCommand = RegisterResult(true, null)
          transferController.handle(conn, registerSuccessMetadata, registerSuccessCommand)
            .map { r2 }
        }.onSuccess {
          promise.complete()
        }.onFailure { error ->
           // new transaction
            log.info("Step 3 - Will register a failed transfer", error)
            val registerFailureMetadata = CommandMetadata(
              stateId = transferId,
              commandId = UuidCreator.getTimeOrdered(),
              correlationId = correlationId)
            val registerFailureCommand = RegisterResult(false, error.message)
            transferController.handle(registerFailureMetadata, registerFailureCommand)
              .onSuccess { promise.complete() }
          }
        }
    return promise.future()
  }

}