package io.github.crabzilla.core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalArgumentException

class CoreTests {

  @Test
  fun allowed() {
    val aggregateRootName = AggregateRootName("0123456789012345")
  }

  @Test
  fun biggerThan16CharsIsNotAllowed() {
    assertThrows<IllegalArgumentException>(IllegalArgumentException::class.java.name) { AggregateRootName("01234567890123456") }
  }
}
