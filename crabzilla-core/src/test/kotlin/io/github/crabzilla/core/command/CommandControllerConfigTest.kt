package io.github.crabzilla.core.command

import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.customerConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CommandControllerConfigTest {

  @Test
  fun stateSerialName() {
      assertEquals(customerConfig.stateSerialName(), Customer.serializer().descriptor.serialName)
  }
}