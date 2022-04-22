package io.github.crabzilla.core.command

import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.customerConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CommandComponentTest {

  @Test
  fun stateSerialName() {
    assertEquals(customerConfig.stateClass, Customer::class)
  }
}
