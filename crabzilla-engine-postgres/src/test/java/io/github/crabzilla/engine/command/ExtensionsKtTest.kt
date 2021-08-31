package io.github.crabzilla.engine.command

import io.github.crabzilla.core.command.CommandHandlerApi
import io.github.crabzilla.core.command.EventHandler
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.customerEventHandler
import org.junit.jupiter.api.Test

internal class ExtensionsKtTest{

  class InvalidCmdHandler(applier: EventHandler<Customer, CustomerEvent>) :
    CommandHandlerApi<Customer, CustomerCommand, CustomerEvent>(applier)

  @Test
  @Throws(UnknownCommandHandler::class)
  fun invalidCmdHandler() {
    val ch = InvalidCmdHandler(customerEventHandler)
    ch.wrap()
  }
}