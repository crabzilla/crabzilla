package io.github.crabzilla.accounts.domain

import io.github.crabzilla.accounts.domain.accounts.Account
import io.github.crabzilla.accounts.domain.accounts.AccountBalanceNotEnough
import io.github.crabzilla.accounts.domain.accounts.AccountCommand
import io.github.crabzilla.accounts.domain.accounts.AccountCommand.DepositMoney
import io.github.crabzilla.accounts.domain.accounts.AccountCommand.OpenAccount
import io.github.crabzilla.accounts.domain.accounts.AccountCommandHandler
import io.github.crabzilla.accounts.domain.accounts.AccountEvent.AccountOpened
import io.github.crabzilla.accounts.domain.accounts.AccountEvent.MoneyDeposited
import io.github.crabzilla.accounts.domain.accounts.AccountEvent.MoneyWithdrawn
import io.github.crabzilla.accounts.domain.accounts.DepositExceeded
import io.github.crabzilla.accounts.domain.accounts.accountEventHandler
import io.github.crabzilla.core.command.CommandControllerConfig
import io.github.crabzilla.core.test.TestSpecification
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

class AccountsSpecsTest : AnnotationSpec() {

  private val id: UUID = UUID.randomUUID()
  private val config = CommandControllerConfig("Account", accountEventHandler, { AccountCommandHandler() })

  @Test
  fun `when opening an account`() {
    TestSpecification(config)
      .whenCommand(OpenAccount(id, "cpf1", "person1"))
      .then { it.state() shouldBe Account(id, "cpf1", "person1") }
      .then { it.events() shouldBe listOf(AccountOpened(id, "cpf1", "person1")) }
  }

  @Test
  fun `when depositing $2000`() {
    TestSpecification(config)
      .givenEvents(AccountOpened(id, "cpf1", "person1"))
      .whenCommand(DepositMoney(2000.00))
      .then { it.state() shouldBe Account(id, "cpf1", "person1", 2000.00) }
      .then {
        it.events() shouldBe listOf(
          AccountOpened(id, "cpf1", "person1"),
          MoneyDeposited(2000.00, 2000.00)
        )
      }
  }

  @Test
  fun `when depositing $2500`() {
    TestSpecification(config)
      .givenEvents(AccountOpened(id, "cpf1", "person1"))
      .then { it.state() shouldBe Account(id, "cpf1", "person1", 0.00) }
      .then {
        val exception = shouldThrow<DepositExceeded> {
          it.whenCommand(DepositMoney(2500.00))
        }
        exception.message shouldBe "Cannot deposit more than 2500.0"
      }
  }

  @Test
  fun `when withdrawing 100 from an account with balance = 110`() {
    TestSpecification(config)
      .givenEvents(AccountOpened(id, "cpf1", "person1"))
      .whenCommand(DepositMoney(110.00))
      .whenCommand(AccountCommand.WithdrawMoney(100.00))
      .then { it.state() shouldBe Account(id, "cpf1", "person1", 10.00) }
      .then {
        it.events() shouldBe listOf(
          AccountOpened(id, "cpf1", "person1"),
          MoneyDeposited(110.00, 110.00),
          MoneyWithdrawn(100.00, 10.00)
        )
      }
  }

  @Test
  fun `when withdrawing 100 from an account with balance = 50`() {
    TestSpecification(config)
      .givenEvents(AccountOpened(id, "cpf1", "person1"))
      .then { it.state() shouldBe Account(id, "cpf1", "person1", 0.00) }
      .then {
        val exception = shouldThrow<AccountBalanceNotEnough> {
          it.whenCommand(AccountCommand.WithdrawMoney(2500.00))
        }
        exception.message shouldBe "Account $id doesn't have enough balance"
      }
  }

}