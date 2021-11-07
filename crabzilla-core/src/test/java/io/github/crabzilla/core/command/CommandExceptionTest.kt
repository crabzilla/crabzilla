package io.github.crabzilla.core.command

import io.github.crabzilla.core.command.CommandException.LockingException
import io.github.crabzilla.core.command.CommandException.UnknownCommandException
import io.github.crabzilla.core.command.CommandException.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CommandExceptionTest {

  @Test
  fun validation() {
    val e = ValidationException(listOf("1", "2"))
    assertThat(e.message).isEqualTo("[1, 2]")
  }

  @Test
  fun locking() {
    val e = LockingException("x")
    assertThat(e.message).isEqualTo("x")
  }

  @Test
  fun unknownCommand() {
    val e = UnknownCommandException("x")
    assertThat(e.message).isEqualTo("x")
  }
}
