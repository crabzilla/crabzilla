package io.github.crabzilla.command

import io.github.crabzilla.command.internal.CommandHandlerWrapper.wrap
import io.github.crabzilla.core.command.CommandException.UnknownCommandException
import io.github.crabzilla.core.command.CommandHandlerApi
import io.github.crabzilla.core.command.EventHandler
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.customerEventHandler
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class CommandHandlerWrapperTest {

  class InvalidCmdHandler(applier: EventHandler<Customer, CustomerEvent>) :
    CommandHandlerApi<Customer, CustomerCommand, CustomerEvent>(applier)

  @Test
  fun invalidCmdHandler() {
    val ch = InvalidCmdHandler(customerEventHandler)
    Assertions.assertThatExceptionOfType(UnknownCommandException::class.java)
      .isThrownBy {
        wrap(ch)
      }.withMessage("Unknown command handler: InvalidCmdHandler")
  }
}