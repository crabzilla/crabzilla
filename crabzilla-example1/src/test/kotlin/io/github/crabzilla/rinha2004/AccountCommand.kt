package io.github.crabzilla.rinha2004

import io.github.crabzilla.rinha2004.CustomerAccountCommand.CommitNewDeposit
import io.github.crabzilla.rinha2004.CustomerAccountCommand.CommitNewWithdraw
import io.github.crabzilla.rinha2004.CustomerAccountCommand.RegisterNewAccount

// commands

sealed interface CustomerAccountCommand {
  data class RegisterNewAccount(val id: Int, val limit: Int, val balance: Int) : CustomerAccountCommand

  data class CommitNewDeposit(val amount: Int, val description: String) : CustomerAccountCommand

  data class CommitNewWithdraw(val amount: Int, val description: String) : CustomerAccountCommand
}

val customerAcctCommandHandler: (state: CustomerAccount, command: CustomerAccountCommand) -> List<CustomerAccountEvent> =
  { state, command ->
    when (command) {
      is RegisterNewAccount -> state.register(id = command.id, limit = command.limit, balance = command.balance)
      is CommitNewDeposit -> state.deposit(amount = command.amount, description = command.description)
      is CommitNewWithdraw -> state.withdraw(amount = command.amount, description = command.description)
    }
  }
