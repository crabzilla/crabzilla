package io.github.crabzilla.rinha2004

import io.github.crabzilla.rinha2004.CustomerAccountCommand.CommitNewDeposit
import io.github.crabzilla.rinha2004.CustomerAccountCommand.CommitNewWithdraw
import io.github.crabzilla.rinha2004.CustomerAccountCommand.RegisterNewAccount
import io.github.crabzilla.rinha2004.CustomerAccountEvent.CustomerAccountRegistered
import io.github.crabzilla.rinha2004.CustomerAccountEvent.DepositCommitted
import io.github.crabzilla.rinha2004.CustomerAccountEvent.WithdrawCommitted

// events
sealed interface CustomerAccountEvent {
  data class CustomerAccountRegistered(val id: Int, val limit: Int, val balance: Int) : CustomerAccountEvent

  data class DepositCommitted(val amount: Int, val description: String, val balance: Int) : CustomerAccountEvent

  data class WithdrawCommitted(val amount: Int, val description: String, val balance: Int) : CustomerAccountEvent
}

val customerAcctEventHandler: (CustomerAccount, CustomerAccountEvent) -> CustomerAccount = {
    state: CustomerAccount, event: CustomerAccountEvent ->
  when (event) {
    is CustomerAccountRegistered -> CustomerAccount(id = event.id, limit = event.limit, balance = event.balance)
    is DepositCommitted -> state.copy(balance = state.balance.plus(event.amount))
    is WithdrawCommitted -> state.copy(balance = state.balance.minus(event.amount))
  }
}

// state

data class LimitExceededException(val amount: Int, val limit: Int) : RuntimeException("Amount $amount exceeds limit $limit")

data class CustomerAccount(val id: Int, val limit: Int, val balance: Int = 0) {
  fun register(
    id: Int,
    limit: Int,
    balance: Int,
  ): List<CustomerAccountEvent> {
    return listOf(CustomerAccountRegistered(id, limit, balance))
  }

  fun deposit(
    amount: Int,
    description: String,
  ): List<CustomerAccountEvent> {
    return listOf(DepositCommitted(amount, description, balance.plus(amount)))
  }

  fun withdraw(
    amount: Int,
    description: String,
  ): List<CustomerAccountEvent> {
    val newBalance = balance.minus(amount)
    if (newBalance < limit) {
      throw LimitExceededException(amount, limit)
    } // look, mother: no else!
    return listOf(WithdrawCommitted(amount, description, newBalance))
  }
}

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
