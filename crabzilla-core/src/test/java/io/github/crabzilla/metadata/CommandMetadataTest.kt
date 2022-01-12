package io.github.crabzilla.metadata

import io.github.crabzilla.core.metadata.CommandMetadata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class CommandMetadataTest {

  @Test
  fun default() {
    val id = UUID.randomUUID()
    val cmdId = UUID.randomUUID()
    val metadata = CommandMetadata(id, cmdId)
    assertThat(metadata.stateId).isEqualTo(id)
    assertThat(metadata.commandId).isEqualTo(cmdId)
    assertThat(metadata.correlationId).isEqualTo(cmdId)
    assertThat(metadata.causationId).isEqualTo(cmdId)
  }

  @Test
  fun t2() {
    val id = UUID.randomUUID()
    val cmdId = UUID.randomUUID()
    val correlationId = UUID.randomUUID()
    val causationId = UUID.randomUUID()
    val metadata = CommandMetadata(id, cmdId, correlationId, causationId)
    assertThat(metadata.stateId).isEqualTo(id)
    assertThat(metadata.commandId).isEqualTo(cmdId)
    assertThat(metadata.correlationId).isEqualTo(correlationId)
    assertThat(metadata.causationId).isEqualTo(causationId)
  }
}
