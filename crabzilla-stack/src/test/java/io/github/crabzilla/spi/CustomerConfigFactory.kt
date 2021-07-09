package io.github.crabzilla.spi

import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.stack.command.CommandControllerConfig

class CustomerConfigFactory : AggregateRootContextProvider<Customer, CustomerCommand, CustomerEvent> {

  override fun create(): AggregateRootContext<Customer, CustomerCommand, CustomerEvent> {
    return CustomerAggregateRootContext()
  }

  class CustomerAggregateRootContext : AggregateRootContext<Customer, CustomerCommand, CustomerEvent> {
    override fun config(): CommandControllerConfig<Customer, CustomerCommand, CustomerEvent> {
      return customerConfig
    }
  }
}
