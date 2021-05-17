package io.github.crabzilla.stack

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class CommandMetadataTest {

  @Test
  fun default() {
    val id = AggregateRootId(UUID.randomUUID())
    val cmdId = CommandId()
    val metadata = CommandMetadata(id, cmdId)
    assertThat(metadata.aggregateRootId).isEqualTo(id)
    assertThat(metadata.commandId).isEqualTo(cmdId)
    assertThat(metadata.correlationId.id).isEqualTo(cmdId.id)
    assertThat(metadata.causationId.id).isEqualTo(cmdId.id)
  }

  @Test
  fun t2() {
    val id = AggregateRootId(UUID.randomUUID())
    val cmdId = CommandId(UUID.randomUUID())
    val correlationId = CorrelationId(UUID.randomUUID())
    val causationId = CausationId(UUID.randomUUID())
    val metadata = CommandMetadata(id, cmdId, correlationId, causationId)
    assertThat(metadata.aggregateRootId).isEqualTo(id)
    assertThat(metadata.commandId).isEqualTo(cmdId)
    assertThat(metadata.correlationId).isEqualTo(correlationId)
    assertThat(metadata.causationId).isEqualTo(causationId)
  }
}
