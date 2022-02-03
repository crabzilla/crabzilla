package io.github.crabzilla.pgclient.projection

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.nio.file.ProviderNotFoundException

internal class EventsProjectorProviderFinderTest {

  @Test
  fun `an unknown provider must fail`() {
    try {
      EventsProjectorProviderFinder().create("?")
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
}
