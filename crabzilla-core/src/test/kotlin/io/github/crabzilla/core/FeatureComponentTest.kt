package io.github.crabzilla.core

import io.github.crabzilla.example1.customer.customerComponent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FeatureComponentTest {

  @Test
  fun stateSerialName() {
    assertEquals(customerComponent.stateClassName(), "Customer")
  }
}
