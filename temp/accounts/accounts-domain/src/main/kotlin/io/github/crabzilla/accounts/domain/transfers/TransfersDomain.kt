package io.github.crabzilla.accounts.domain.transfers

import io.github.crabzilla.accounts.domain.transfers.TransferCommand.RegisterResult
import io.github.crabzilla.accounts.domain.transfers.TransferCommand.RequestTransfer
import io.github.crabzilla.accounts.domain.transfers.TransferEvent.TransferConcluded
import io.github.crabzilla.accounts.domain.transfers.TransferEvent.TransferRequested
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State
import io.github.crabzilla.core.command.CommandHandler
import io.github.crabzilla.core.command.CommandSession
import io.github.crabzilla.core.command.EventHandler
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
sealed class TransferEvent : Event {

  @Serializable
  @SerialName("TransferRequested")
  data class TransferRequested(@Contextual val id: UUID,
                               val amount: Double = 0.00,
                               @Contextual val fromAccountId: UUID,
                               @Contextual val toAccountId: UUID) : TransferEvent()
  @Serializable
  @SerialName("TransferConcluded")
  data class TransferConcluded(val succeeded: Boolean, val errorMessage: String?) : TransferEvent()

}

@Serializable
sealed class TransferCommand : Command {

  @Serializable
  @SerialName("RequestTransfer")
  data class RequestTransfer(@Contextual val id: UUID,
                             val amount: Double = 0.00,
                             @Contextual val fromAccountId: UUID,
                             @Contextual val toAccountId: UUID) : TransferCommand()

  @Serializable
  @SerialName("RegisterResult")
  data class RegisterResult(val succeeded: Boolean, val errorMessage: String?) : TransferCommand()

}

@Serializable
@SerialName("Transfer")
data class Transfer(
  @Contextual val id: UUID,
  val amount: Double = 0.00,
  @Contextual val fromAccountId: UUID,
  @Contextual val toAccountId: UUID,
  val succeeded: Boolean?,
  val errorMessage: String?
) : State {
  companion object {
    fun fromEvent(event: TransferRequested): Transfer {
      return Transfer(id = event.id, amount = event.amount, fromAccountId =  event.fromAccountId,
        toAccountId = event.toAccountId, succeeded = null, errorMessage = null)
    }
  }
}

val transferEventHandler = EventHandler<Transfer, TransferEvent> { state, event ->
  when (event) {
    is TransferRequested -> Transfer.fromEvent(event)
    is TransferConcluded -> state!!.copy(succeeded = event.succeeded, errorMessage = event.errorMessage)
  }
}

class TransferAlreadyExists(id: UUID) : IllegalArgumentException("Transfer $id already exists")
class TransferNotFound : NullPointerException("Transfer not found")

class TransferCommandHandler : CommandHandler<Transfer, TransferCommand, TransferEvent>(transferEventHandler) {

  companion object {
    private fun request(id: UUID,
                        amount: Double = 0.00,
                        fromAccountId: UUID,
                        toAccountId: UUID): List<TransferEvent> {
      return listOf(TransferRequested(id, amount, fromAccountId, toAccountId))
    }
  }

  override fun handleCommand(
    command: TransferCommand,
    state: Transfer?,
  )
          : CommandSession<Transfer, TransferEvent> {
    return when (command) {
      is RequestTransfer -> {
        if (state != null) throw TransferAlreadyExists(command.id)
        withNew(request(command.id, command.amount, command.fromAccountId, command.toAccountId))
      }
      is RegisterResult -> {
        if (state == null) throw TransferNotFound() // TODO should have transferId
        with(state).execute {
          listOf(TransferConcluded(command.succeeded, command.errorMessage))
        }
      }
    }
  }
}

