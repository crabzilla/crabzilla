package io.github.crabzilla.rinha2004

// state

data class LimitExceededException(val amount: Int, val limit: Int) : RuntimeException("Amount $amount exceeds limit $limit")

data class CustomerAccount(val id: Int, val limit: Int, val balance: Int = 0) {
  fun register(
    id: Int,
    limit: Int,
    balance: Int,
  ): List<CustomerAccountEvent> {
    return listOf(CustomerAccountEvent.CustomerAccountRegistered(id = id, limit = limit, balance = balance))
  }

  fun deposit(
    amount: Int,
    description: String,
  ): List<CustomerAccountEvent> {
    return listOf(
      CustomerAccountEvent.DepositCommitted(
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
      CustomerAccountEvent.WithdrawCommitted(
        amount = amount,
        description = description,
        balance = newBalance,
      ),
    )
  }
}
