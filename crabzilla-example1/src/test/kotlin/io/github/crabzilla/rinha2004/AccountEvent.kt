package io.github.crabzilla.rinha2004

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
