package io.github.crabzilla.projection.verticle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.ProviderNotFoundException

@DisplayName("Instantiating EventsProjectorProvider")
internal class EventProjectorProviderFactoryTest {

  @Test
  fun `an unknown provider must fail`() {
    try {
      EventProjectorProviderFactory().create("?")
      fail("Should fail")
    } catch (e: Exception) {
      when (e) {
        is ProviderNotFoundException -> {
          assertEquals("Provider ? not found", e.message)
        }
        else ->
          fail("Should be ProviderNotFoundException")
      }
    }
  }

  @Test
  fun `a provider must work`() {
    EventProjectorProviderFactory().create("io.github.crabzilla.example1.customer.CustomersProjectorFactory")
  }
}
