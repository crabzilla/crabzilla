package io.github.crabzilla.accounts.domain.accounts

import io.github.crabzilla.accounts.domain.accounts.AccountCommand.DepositMoney
import io.github.crabzilla.accounts.domain.accounts.AccountCommand.OpenAccount
import io.github.crabzilla.accounts.domain.accounts.AccountCommand.WithdrawMoney
import io.github.crabzilla.accounts.domain.accounts.AccountEvent.AccountOpened
import io.github.crabzilla.accounts.domain.accounts.AccountEvent.MoneyDeposited
import io.github.crabzilla.accounts.domain.accounts.AccountEvent.MoneyWithdrawn
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State
import io.github.crabzilla.json.javaModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

object AccountsSerialization {

  @kotlinx.serialization.ExperimentalSerializationApi
  val accountModule = SerializersModule {
    include(javaModule)
    polymorphic(State::class) {
      subclass(Account::class, Account.serializer())
    }
    polymorphic(Command::class) {
      subclass(OpenAccount::class, OpenAccount.serializer())
      subclass(DepositMoney::class, DepositMoney.serializer())
      subclass(WithdrawMoney::class, WithdrawMoney.serializer())
    }
    polymorphic(Event::class) {
      subclass(AccountOpened::class, AccountOpened.serializer())
      subclass(MoneyDeposited::class, MoneyDeposited.serializer())
      subclass(MoneyWithdrawn::class, MoneyWithdrawn.serializer())
    }
  }

}