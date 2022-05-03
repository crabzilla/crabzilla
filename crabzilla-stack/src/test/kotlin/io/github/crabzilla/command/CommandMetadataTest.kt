package io.github.crabzilla.command

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

internal class CommandMetadataTest {

  @Test
  fun `new with stateId`() {
    val stateId = UUID.randomUUID()
    val m = CommandMetadata.new(stateId)
    assertEquals(stateId, m.stateId)
    assertEquals(m.correlationId, m.commandId)
    assertEquals(m.causationId, m.commandId)
  }

  @Test
  fun `new with stateId, causationId and correlationID`() {
    val stateId = UUID.randomUUID()
    val correlationID = UUID.randomUUID()
    val causationId = UUID.randomUUID()
    val m = CommandMetadata.new(stateId, correlationID, causationId)
    assertEquals(stateId, m.stateId)
    assertEquals(m.correlationId, correlationID)
    assertEquals(m.causationId, causationId)
  }

  @Test
  fun `to and from JsonObject`() {
    val metadata = CommandMetadata.new(UUID.randomUUID())
    val json = metadata.toJsonObject()
    println(json.encodePrettily())
    assertEquals(json, CommandMetadata.fromJsonObject(json).toJsonObject())
  }

}
