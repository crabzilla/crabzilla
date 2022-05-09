package io.github.crabzilla

import io.github.crabzilla.EventRecord.Companion.fromJsonObject
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRegistered
import io.github.crabzilla.example1.customer.CustomerJsonObjectSerDer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

@DisplayName("Serializing EventRecord")
internal class EventRecordTest {

  val id: UUID = UUID.fromString("c2aeadc1-d6b5-4df6-82a4-7dec4f1df429")
  val event = CustomerRegistered(id, "customer1") as CustomerEvent
  private val eventAsJson = CustomerJsonObjectSerDer().eventToJson(event)
  private val eventMetadata = EventMetadata(
    stateType = "Customer",
    stateId = id,
    eventId = UUID.randomUUID(),
    correlationId = UUID.randomUUID(),
    causationId = UUID.randomUUID(),
    eventSequence = 1,
    version = 1,
    eventType = CustomerRegistered::class.java.simpleName
  )

  @Test
  fun toJson() {
    val eventRecord = EventRecord(eventMetadata, eventAsJson)
    val eventRecordAsJson = eventRecord.toJsonObject()
    assertThat(fromJsonObject(eventRecordAsJson)).isEqualTo(eventRecord)
  }
}
