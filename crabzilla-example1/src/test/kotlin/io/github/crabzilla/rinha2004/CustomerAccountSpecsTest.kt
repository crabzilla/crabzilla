package io.github.crabzilla.rinha2004

import io.github.crabzilla.core.CrabzillaCommandsSession
import io.github.crabzilla.core.TestSpecification
import io.github.crabzilla.rinha2004.CustomerAccountCommand.CommitNewDeposit
import io.github.crabzilla.rinha2004.CustomerAccountCommand.CommitNewWithdraw
import io.github.crabzilla.rinha2004.CustomerAccountCommand.RegisterNewAccount
import io.github.crabzilla.rinha2004.CustomerAccountEvent.CustomerAccountRegistered
import io.github.crabzilla.rinha2004.CustomerAccountEvent.WithdrawCommitted
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Customer account scenarios - Rinha 2004")
class CustomerAccountSpecsTest {
  private val id = 1
  private lateinit var session: CrabzillaCommandsSession<CustomerAccountCommand, CustomerAccount, CustomerAccountEvent>

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
        assertThat(it.appliedEvents()).isEqualTo(
          listOf(
            CustomerAccountRegistered(
              id,
              limit = 10,
              balance = 5,
            ),
          ),
        )
      }
      .then { // assert state
        assertThat(it.currentState()).isEqualTo(CustomerAccount(id, limit = 10, balance = 5))
      }
  }

  @Test
  fun `given a RegisterNewAccount $5 then a DepositCommitted $20, the state and events are correct`() {
    TestSpecification(session)
      .whenCommand(RegisterNewAccount(id, limit = 10, balance = 5))
      .whenCommand(CommitNewDeposit(amount = 20, description = "ya ya"))
      .then { // assert events
        assertThat(it.appliedEvents()).isEqualTo(
          listOf(
            CustomerAccountRegistered(
              id,
              limit = 10,
              balance = 5,
            ),
            CustomerAccountEvent.DepositCommitted(
              amount = 20,
              description = "ya ya",
              balance = 25,
            ),
          ),
        )
      }
      .then { // assert state
        assertThat(it.currentState()).isEqualTo(CustomerAccount(id, limit = 10, balance = 25))
      }
  }

  @Test
  fun `given a RegisterNewAccount $5 then a WithdrawCommitted $10, the state and events are correct`() {
    TestSpecification(session)
      .whenCommand(RegisterNewAccount(id, limit = 50, balance = 100))
      .whenCommand(CommitNewWithdraw(amount = 10, description = "ya ya"))
      .then { // assert events
        assertThat(it.appliedEvents()).isEqualTo(
          listOf(
            CustomerAccountRegistered(
              id,
              limit = 50,
              balance = 100,
            ),
            WithdrawCommitted(
              amount = 10,
              description = "ya ya",
              balance = 90,
            ),
          ),
        )
      }
      .then { // assert state
        assertThat(it.currentState()).isEqualTo(CustomerAccount(id, limit = 50, balance = 90))
      }
  }

  @Test
  fun `given a RegisterNewAccount $5 then a WithdrawCommitted $100, the state and events are correct`() {
    TestSpecification(session)
      .whenCommand(RegisterNewAccount(id, limit = 0, balance = 5))
      .whenCommand(CommitNewWithdraw(amount = 10, description = "ya ya"))
      .then {
        // assert exception
        assertThat(it.lastException()).isEqualTo(LimitExceededException(amount = 10, limit = 0))
      }
      .then {
        // assert events
        assertThat(it.appliedEvents()).isEqualTo(
          listOf(
            CustomerAccountRegistered(
              id,
              limit = 0,
              balance = 5,
            ),
          ),
        )
      }
      .then {
        // assert state
        assertThat(it.currentState()).isEqualTo(CustomerAccount(id, limit = 0, balance = 5))
      }
  }
}
