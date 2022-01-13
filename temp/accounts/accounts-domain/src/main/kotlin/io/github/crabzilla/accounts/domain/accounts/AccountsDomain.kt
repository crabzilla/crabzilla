package io.github.crabzilla.accounts.domain.accounts

import io.github.crabzilla.accounts.domain.accounts.AccountCommand.DepositMoney
import io.github.crabzilla.accounts.domain.accounts.AccountCommand.OpenAccount
import io.github.crabzilla.accounts.domain.accounts.AccountCommand.WithdrawMoney
import io.github.crabzilla.accounts.domain.accounts.AccountEvent.AccountOpened
import io.github.crabzilla.accounts.domain.accounts.AccountEvent.MoneyDeposited
import io.github.crabzilla.accounts.domain.accounts.AccountEvent.MoneyWithdrawn
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
sealed class AccountEvent : Event {
  @Serializable
  @SerialName("AccountOpened")
  data class AccountOpened(@Contextual val id: UUID, val cpf: String, val name: String) : AccountEvent()

  @Serializable
  @SerialName("MoneyDeposited")
  data class MoneyDeposited(val amount: Double, val finalBalance: Double) : AccountEvent()

  @Serializable
  @SerialName("MoneyWithdrawn")
  data class MoneyWithdrawn(val amount: Double, val finalBalance: Double) : AccountEvent()

}

@Serializable
sealed class AccountCommand : Command {
  @Serializable
  @SerialName("OpenAccount")
  data class OpenAccount(@Contextual val id: UUID, val cpf: String, val name: String) : AccountCommand()

  @Serializable
  @SerialName("DepositMoney")
  data class DepositMoney(val amount: Double) : AccountCommand()

  @Serializable
  @SerialName("WithdrawMoney")
  data class WithdrawMoney(val amount: Double) : AccountCommand()

}

@Serializable
@SerialName("Account")
data class Account(
  @Contextual val id: UUID,
  val cpf: String,
  val name: String,
  val balance: Double = 0.00
) : State {
  companion object {
    fun fromEvent(event: AccountOpened): Account {
      return Account(id = event.id, cpf = event.cpf, name = event.name)
    }
  }
}

val accountEventHandler = EventHandler<Account, AccountEvent> { state, event ->
  when (event) {
    is AccountOpened -> Account.fromEvent(event)
    is MoneyDeposited -> state!!.copy(balance = state.balance + event.amount)
    is MoneyWithdrawn -> state!!.copy(balance = state.balance - event.amount)
  }
}

class AccountAlreadyExists(id: UUID) : IllegalArgumentException("Account $id already exists")
class AccountNotFound : NullPointerException("Account not found")
class AccountBalanceNotEnough(id: UUID) : IllegalStateException("Account $id doesn't have enough balance")
class DepositExceeded(amount: Double) : IllegalStateException("Cannot deposit more than $amount")

class AccountCommandHandler : CommandHandler<Account, AccountCommand, AccountEvent>(accountEventHandler) {

  companion object {

    private const val LIMIT = 2000.00

    private fun open(id: UUID, cpf: String, name: String): List<AccountEvent> {
      return listOf(AccountOpened(id = id, cpf, name))
    }

    private fun Account.deposit(amount: Double): List<AccountEvent> {
      if (amount > LIMIT) {
        throw DepositExceeded(amount)
      }
      return listOf(MoneyDeposited(amount, balance + amount))
    }

    private fun Account.withdraw(amount: Double): List<AccountEvent> {
      if (balance < amount) throw AccountBalanceNotEnough(id)
      return listOf(MoneyWithdrawn(amount, balance - amount))
    }

  }

  override fun handleCommand(
    command: AccountCommand,
    state: Account?,
  )
          : CommandSession<Account, AccountEvent> {
    return when (command) {
      is OpenAccount -> {
        if (state != null) throw AccountAlreadyExists(command.id)
        withNew(open(command.id, command.cpf, command.name))
      }
      else -> {
        if (state == null) throw AccountNotFound()
        when (command) {
          is DepositMoney -> with(state).execute { it.deposit(command.amount) }
          is WithdrawMoney -> with(state).execute { it.withdraw(command.amount) }
          else -> throw IllegalArgumentException("Invalid command")
        }
      }
    }
  }
}
