package io.github.crabzilla.core

import io.github.crabzilla.core.command.CommandException
import io.github.crabzilla.core.command.CommandException.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class CommandExceptionTest {

  @Test
  fun validation() {
    assertThat(ValidationException(listOf("error 1", "error 2")))
      .isInstanceOf(CommandException::class.java)
  }

  @Test
  fun lock() {
    assertThat(CommandException.LockingException("error 3"))
      .isInstanceOf(CommandException::class.java)
  }
}
