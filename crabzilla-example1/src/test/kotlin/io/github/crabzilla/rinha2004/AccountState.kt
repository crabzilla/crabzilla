package io.github.crabzilla.rinha2004

import io.github.crabzilla.rinha2004.CustomerAccountEvent.CustomerAccountRegistered
import io.github.crabzilla.rinha2004.CustomerAccountEvent.DepositCommitted
import io.github.crabzilla.rinha2004.CustomerAccountEvent.WithdrawCommitted
import java.time.LocalDateTime

// state

data class LimitExceededException(val amount: Int, val limit: Int) :
  RuntimeException("Amount $amount exceeds limit $limit")

fun <T> withMaxSize(maxSize: Int): MutableList<T> {
  // require(maxSize > 0) { "maxSize must be greater than 0" }
  return object : ArrayList<T>(maxSize) {
    override fun add(element: T): Boolean {
      if (size >= maxSize) {
        removeAt(0) // Remove the oldest element
      }
      return super.add(element)
    }
  }
}

data class CustomerAccount(
  val id: Int,
  val limit: Int,
  val balance: Int = 0,
  val lastTenTransactions: MutableList<CustomerAccountEvent> = withMaxSize(10),
) {
  // This state model have the last 10 transactions. Just to skip read/view model and optimize it for perf.

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
      DepositCommitted(amount = amount, description = description, balance = balance.plus(amount), LocalDateTime.now()),
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
      WithdrawCommitted(amount = amount, description = description, balance = newBalance, LocalDateTime.now()),
    )
  }
}
