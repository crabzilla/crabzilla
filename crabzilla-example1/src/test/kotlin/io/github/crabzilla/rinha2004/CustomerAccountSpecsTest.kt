package io.github.crabzilla.rinha2004

import io.github.crabzilla.core.CrabzillaCommandsSession
import io.github.crabzilla.core.TestSpecification
import io.github.crabzilla.rinha2004.CustomerAccountCommand.CommitNewDeposit
import io.github.crabzilla.rinha2004.CustomerAccountCommand.CommitNewWithdraw
import io.github.crabzilla.rinha2004.CustomerAccountCommand.RegisterNewAccount
import io.github.crabzilla.rinha2004.CustomerAccountEvent.CustomerAccountRegistered
import io.github.crabzilla.rinha2004.CustomerAccountEvent.DepositCommitted
import io.github.crabzilla.rinha2004.CustomerAccountEvent.WithdrawCommitted
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Customer account scenarios - Rinha 2004")
class CustomerAccountSpecsTest {
  private val id = 1
  private lateinit var session: CrabzillaCommandsSession<CustomerAccountCommand, CustomerAccount, CustomerAccountEvent>

  private fun CustomerAccount.shouldBe(customerAccount: CustomerAccount) {
    try {
      this.shouldBeEqualToIgnoringFields(customerAccount, CustomerAccount::lastTenTransactions)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun CustomerAccountEvent.shouldBe(customerAccountEvent: CustomerAccountEvent) {
    try {
      this.shouldBeEqualToIgnoringFields(customerAccountEvent, CustomerAccountEvent::date)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  @BeforeEach
  fun setup() {
    session =
      CrabzillaCommandsSession(
        originalState = CustomerAccount(id = 0, limit = 0, balance = 0),
        eventHandler = customerAcctEventHandler,
        commandHandler = customerAcctCommandHandler,
      )
  }

  @Test
  fun `given a RegisterNewAccount command, the state and events are correct`() {
    TestSpecification(session)
      .whenCommand(RegisterNewAccount(id, limit = 10, balance = 5))
      .then { // assert events
        it.appliedEvents()[0].shouldBe(CustomerAccountRegistered(id, limit = 10, balance = 5))
      }
      .then { // assert state
        it.currentState().shouldBe(CustomerAccount(id, limit = 10, balance = 5))
      }
  }

  @Test
  fun `given a RegisterNewAccount then a DepositCommitted $20, the state and events are correct`() {
    TestSpecification(session)
      .whenCommand(RegisterNewAccount(id, limit = 10, balance = 5))
      .whenCommand(CommitNewDeposit(amount = 20, description = "ya ya"))
      .then { // assert events
        it.appliedEvents()[0].shouldBe(CustomerAccountRegistered(id, limit = 10, balance = 5))
        it.appliedEvents()[1].shouldBe(DepositCommitted(amount = 20, description = "ya ya", balance = 25))
      }
      .then { // assert state
        it.currentState().shouldBe(CustomerAccount(id, limit = 10, balance = 25))
        it.appliedEvents()[1].shouldBe(it.currentState().lastTenTransactions[0])
      }
  }

  @Test
  fun `given a RegisterNewAccount then a WithdrawCommitted $10, the state and events are correct`() {
    TestSpecification(session)
      .whenCommand(RegisterNewAccount(id, limit = 50, balance = 100))
      .whenCommand(CommitNewWithdraw(amount = 10, description = "ya ya"))
      .then {
        it.appliedEvents()[0].shouldBe(CustomerAccountRegistered(id, limit = 50, balance = 100))
        it.appliedEvents()[1].shouldBe(WithdrawCommitted(amount = 10, description = "ya ya", balance = 90))
      }
      .then { // assert state
        it.currentState().shouldBe(CustomerAccount(id, limit = 50, balance = 90))
      }
  }

  @Test
  fun `given a RegisterNewAccount $5 then a WithdrawCommitted $100, the state and events are correct`() {
    TestSpecification(session)
      .whenCommand(RegisterNewAccount(id, limit = 0, balance = 5))
      .whenCommand(CommitNewWithdraw(amount = 10, description = "ya ya"))
      .then {
        // assert exception
        it.lastException() shouldBe LimitExceededException(amount = 10, limit = 0)
      }
      .then {
        // assert events
        it.appliedEvents()[0].shouldBe(CustomerAccountRegistered(id, limit = 0, balance = 5))
      }
      .then {
        // assert state
        it.currentState().shouldBe(CustomerAccount(id, limit = 0, balance = 5))
      }
  }

  @Test
  fun `given a RegisterNewAccount then 10 DepositCommitted then 5 WithdrawCommitted $1, the state and events are correct`() {
    TestSpecification(session)
      .whenCommand(RegisterNewAccount(id, limit = 0, balance = 0))
      .whenCommand(CommitNewDeposit(amount = 1, description = "ya ya"))
      .whenCommand(CommitNewDeposit(amount = 2, description = "ya ya"))
      .whenCommand(CommitNewDeposit(amount = 3, description = "ya ya"))
      .whenCommand(CommitNewDeposit(amount = 4, description = "ya ya"))
      .whenCommand(CommitNewDeposit(amount = 5, description = "ya ya"))
      .whenCommand(CommitNewDeposit(amount = 6, description = "ya ya"))
      .whenCommand(CommitNewDeposit(amount = 7, description = "ya ya"))
      .whenCommand(CommitNewDeposit(amount = 8, description = "ya ya"))
      .whenCommand(CommitNewDeposit(amount = 9, description = "ya ya"))
      .whenCommand(CommitNewDeposit(amount = 10, description = "ya ya"))
      .whenCommand(CommitNewWithdraw(amount = 11, description = "ya ya"))
      .whenCommand(CommitNewWithdraw(amount = 12, description = "ya ya"))
      .whenCommand(CommitNewWithdraw(amount = 13, description = "ya ya"))
      .whenCommand(CommitNewWithdraw(amount = 14, description = "ya ya"))
      .whenCommand(CommitNewWithdraw(amount = 15, description = "ya ya"))
      .then {
        it.appliedEvents().size shouldBe 15
      }
      .then { // assert state

        // assert state
        it.currentState().shouldBe(CustomerAccount(id, limit = 0, balance = 5))

        val stateTransactions = it.currentState().lastTenTransactions
        stateTransactions.size shouldBe 10
        stateTransactions[0].shouldBe(DepositCommitted(amount = 5, description = "ya ya", balance = 15))
        stateTransactions[1].shouldBe(DepositCommitted(amount = 6, description = "ya ya", balance = 21))
        stateTransactions[2].shouldBe(DepositCommitted(amount = 7, description = "ya ya", balance = 28))
        stateTransactions[3].shouldBe(DepositCommitted(amount = 8, description = "ya ya", balance = 36))
        stateTransactions[4].shouldBe(DepositCommitted(amount = 9, description = "ya ya", balance = 45))
        stateTransactions[5].shouldBe(DepositCommitted(amount = 10, description = "ya ya", balance = 55))
        stateTransactions[6].shouldBe(WithdrawCommitted(amount = 11, description = "ya ya", balance = 44))
        stateTransactions[7].shouldBe(WithdrawCommitted(amount = 12, description = "ya ya", balance = 32))
        stateTransactions[8].shouldBe(WithdrawCommitted(amount = 13, description = "ya ya", balance = 19))
        stateTransactions[9].shouldBe(WithdrawCommitted(amount = 14, description = "ya ya", balance = 5))

        // assert exception
        it.lastException() shouldBe LimitExceededException(amount = 15, limit = 0)
      }
  }
}
