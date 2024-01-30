package io.github.crabzilla

import io.github.crabzilla.context.EventMetadata
import io.github.crabzilla.context.EventRecord
import io.github.crabzilla.context.EventRecord.Companion.fromJsonObject
import io.github.crabzilla.context.EventRecord.Companion.toJsonArray
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRegistered
import io.github.crabzilla.example1.customer.CustomerEventSerDer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*

@DisplayName("Serializing io.github.crabzilla.context.EventRecord")
class EventRecordTest {
  private val id = UUID.randomUUID()
  private val event = CustomerRegistered(id, "John") as CustomerEvent
  private val eventAsJson = CustomerEventSerDer().toJson(event)
  private val eventMetadata =
    EventMetadata(
      streamId = 1,
      stateType = "Customer",
      stateId = id.toString(),
      eventId = UUID.randomUUID(),
      correlationId = UUID.randomUUID(),
      causationId = UUID.randomUUID(),
      eventSequence = 1,
      version = 1,
      eventType = CustomerRegistered::class.java.simpleName,
    )

  @Test
  fun toJson() {
    val eventRecord = EventRecord(eventMetadata, eventAsJson)
    val eventRecordAsJson = eventRecord.toJsonObject()
    assertThat(fromJsonObject(eventRecordAsJson)).isEqualTo(eventRecord)
  }

  @Test
  fun listToJsonArray() {
    val eventRecord = EventRecord(eventMetadata, eventAsJson)
    val list = listOf(eventRecord)
    val eventRecordAsJson = eventRecord.toJsonObject()
    val jsonArray = list.toJsonArray()
    assertThat(eventRecordAsJson).isEqualTo(jsonArray.first())
  }
}
