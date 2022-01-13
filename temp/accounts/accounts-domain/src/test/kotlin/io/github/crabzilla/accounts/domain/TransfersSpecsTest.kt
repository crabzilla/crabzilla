package io.github.crabzilla.accounts.domain

import io.github.crabzilla.accounts.domain.transfers.Transfer
import io.github.crabzilla.accounts.domain.transfers.TransferCommand
import io.github.crabzilla.accounts.domain.transfers.TransferCommandHandler
import io.github.crabzilla.accounts.domain.transfers.TransferEvent
import io.github.crabzilla.accounts.domain.transfers.TransferEvent.TransferConcluded
import io.github.crabzilla.accounts.domain.transfers.TransferEvent.TransferRequested
import io.github.crabzilla.accounts.domain.transfers.transferEventHandler
import io.github.crabzilla.core.command.CommandControllerConfig
import io.github.crabzilla.core.test.TestSpecification
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

class TransfersSpecsTest : AnnotationSpec() {

  companion object {
    private val id: UUID = UUID.randomUUID()
    private val fromAcctId: UUID = UUID.randomUUID()
    private val toAcctId: UUID = UUID.randomUUID()
    private val config = CommandControllerConfig("Transfer", transferEventHandler, { TransferCommandHandler() })
  }

  @Test
  fun `when requesting a transfer of 100`() {
    TestSpecification(config)
      .whenCommand(TransferCommand.RequestTransfer(id, 100.00, fromAcctId, toAcctId))
      .then { it.state() shouldBe Transfer(id, 100.00, fromAcctId, toAcctId, null, null) }
      .then { it.events() shouldBe listOf(TransferRequested(id, 100.00, fromAcctId, toAcctId)) }
  }

  @Test
  fun `when registering a successful transfer`() {
    TestSpecification(config)
      .whenCommand(TransferCommand.RequestTransfer(id, 100.00, fromAcctId, toAcctId))
      .whenCommand(TransferCommand.RegisterResult(true, null))
      .then { it.state() shouldBe Transfer(id, 100.00, fromAcctId, toAcctId, true, null) }
      .then { it.events() shouldBe listOf(
        TransferRequested(id, 100.00, fromAcctId, toAcctId),
        TransferConcluded(true, null))
      }
  }

  @Test
  fun `when registering a failed transfer`() {
    TestSpecification(config)
      .whenCommand(TransferCommand.RequestTransfer(id, 100.00, fromAcctId, toAcctId))
      .whenCommand(TransferCommand.RegisterResult(false, "an error x"))
      .then { it.state() shouldBe Transfer(id, 100.00, fromAcctId, toAcctId, false,
        "an error x") }
      .then { it.events() shouldBe listOf(
        TransferRequested(id, 100.00, fromAcctId, toAcctId),
        TransferConcluded(false, "an error x")
        )
      }
  }

}