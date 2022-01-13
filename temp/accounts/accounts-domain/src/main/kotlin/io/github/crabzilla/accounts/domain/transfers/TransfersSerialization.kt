package io.github.crabzilla.accounts.domain.transfers

import io.github.crabzilla.accounts.domain.accounts.AccountsSerialization.accountModule
import io.github.crabzilla.accounts.domain.transfers.TransferCommand.RegisterResult
import io.github.crabzilla.accounts.domain.transfers.TransferCommand.RequestTransfer
import io.github.crabzilla.accounts.domain.transfers.TransferEvent.TransferConcluded
import io.github.crabzilla.accounts.domain.transfers.TransferEvent.TransferRequested
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.Event
import io.github.crabzilla.core.State
import io.github.crabzilla.json.javaModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

object TransfersSerialization {

  /**
   * kotlinx.serialization
   */
  @kotlinx.serialization.ExperimentalSerializationApi
  val transferModule = SerializersModule {
    include(javaModule)
    include(accountModule)
    polymorphic(State::class) {
      subclass(Transfer::class, Transfer.serializer())
    }
    polymorphic(Command::class) {
      subclass(RequestTransfer::class, RequestTransfer.serializer())
      subclass(RegisterResult::class, RegisterResult.serializer())
    }
    polymorphic(Event::class) {
      subclass(TransferRequested::class, TransferRequested.serializer())
      subclass(TransferConcluded::class, TransferConcluded.serializer())
    }
  }

}