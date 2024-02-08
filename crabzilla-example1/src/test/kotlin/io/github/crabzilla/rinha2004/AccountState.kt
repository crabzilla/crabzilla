package io.github.crabzilla.rinha2004

import io.github.crabzilla.rinha2004.CustomerAccountEvent.CustomerAccountRegistered
import io.github.crabzilla.rinha2004.CustomerAccountEvent.DepositCommitted
import io.github.crabzilla.rinha2004.CustomerAccountEvent.WithdrawCommitted

// state

data class LimitExceededException(val amount: Int, val limit: Int) : RuntimeException("Amount $amount exceeds limit $limit")

data class CustomerAccount(val id: Int, val limit: Int, val balance: Int = 0) {
  fun register(
    id: Int,
    limit: Int,
    balance: Int,
  ): List<CustomerAccountEvent> {
    return listOf(CustomerAccountRegistered(id = id, limit = limit, balance = balance))
  }

  fun deposit(
    amount: Int,
    description: String,
  ): List<CustomerAccountEvent> {
    return listOf(
      DepositCommitted(
        amount = amount,
        description = description,
        balance = balance.plus(amount),
      ),
    )
  }

  fun withdraw(
    amount: Int,
    description: String,
  ): List<CustomerAccountEvent> {
    val newBalance = balance.minus(amount)
    if (newBalance < limit) {
      throw LimitExceededException(amount, limit)
    } // look, mother: no else!
    return listOf(
      WithdrawCommitted(
        amount = amount,
        description = description,
        balance = newBalance,
      ),
    )
  }
}
