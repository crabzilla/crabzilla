package io.github.crabzilla.spi

import io.github.crabzilla.example1.Customer
import io.github.crabzilla.example1.CustomerCommand
import io.github.crabzilla.example1.CustomerEvent
import io.github.crabzilla.example1.customerConfig
import io.github.crabzilla.stack.AggregateRootConfig

class CustomerConfigFactory : AggregateRootContextProvider<Customer, CustomerCommand, CustomerEvent> {

  override fun create(): AggregateRootContext<Customer, CustomerCommand, CustomerEvent> {
    return CustomerAggregateRootContext()
  }

  class CustomerAggregateRootContext : AggregateRootContext<Customer, CustomerCommand, CustomerEvent> {
    override fun config(): AggregateRootConfig<Customer, CustomerCommand, CustomerEvent> {
      return customerConfig
    }
  }
}
