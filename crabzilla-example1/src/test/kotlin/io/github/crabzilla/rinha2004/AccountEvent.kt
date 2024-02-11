package io.github.crabzilla.rinha2004

import io.github.crabzilla.rinha2004.CustomerAccountEvent.CustomerAccountRegistered
import io.github.crabzilla.rinha2004.CustomerAccountEvent.DepositCommitted
import io.github.crabzilla.rinha2004.CustomerAccountEvent.WithdrawCommitted
import java.time.LocalDateTime

// events
sealed class CustomerAccountEvent(open val date: LocalDateTime) {
  data class CustomerAccountRegistered(
    val id: Int,
    val limit: Int,
    val balance: Int,
    override val date: LocalDateTime = LocalDateTime.now(),
  ) :
    CustomerAccountEvent(date)

  data class DepositCommitted(
    val amount: Int,
    val description: String,
    val balance: Int,
    override val date: LocalDateTime = LocalDateTime.now(),
  ) : CustomerAccountEvent(date)

  data class WithdrawCommitted(
    val amount: Int,
    val description: String,
    val balance: Int,
    override val date: LocalDateTime = LocalDateTime.now(),
  ) : CustomerAccountEvent(date)
}

val customerAcctEventHandler: (CustomerAccount, CustomerAccountEvent) -> CustomerAccount = {
    state: CustomerAccount, event: CustomerAccountEvent ->
  fun newList(): MutableList<CustomerAccountEvent> {
//    println("---- before ${state.lastTenTransactions.size}")
    state.lastTenTransactions.add(event)
//    state.lastTenTransactions.forEachIndexed { index, event ->
//      println("Event#$index: $event")
//    }
//    println("     after ${state.lastTenTransactions.size}")
    return state.lastTenTransactions
  }
  when (event) {
    is CustomerAccountRegistered ->
      CustomerAccount(id = event.id, limit = event.limit, balance = event.balance)
    is DepositCommitted -> state.copy(balance = state.balance.plus(event.amount), lastTenTransactions = newList())
    is WithdrawCommitted -> state.copy(balance = state.balance.minus(event.amount), lastTenTransactions = newList())
  }
}
