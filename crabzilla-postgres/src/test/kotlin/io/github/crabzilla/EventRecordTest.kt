package io.github.crabzilla

import CustomerEventsSerDer
import EventMetadata
import EventRecord
import EventRecord.Companion.fromJsonObject
import EventRecord.Companion.toJsonArray
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRegistered
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ulid4j.Ulid

@DisplayName("Serializing EventRecord")
internal class EventRecordTest {
  private val id: String = "c2aeadc1-d6b5-4df6-82a4-7dec4f1df429"
  private val event = CustomerRegistered(id, "John") as CustomerEvent
  private val eventAsJson = CustomerEventsSerDer().toJson(event)
  private val eventMetadata =
    EventMetadata(
      stateType = "Customer",
      stateId = id,
      eventId = ulidFunction.invoke(),
      correlationId = ulidFunction.invoke(),
      causationId = ulidFunction.invoke(),
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

  companion object {
    val ulidGenerator = Ulid()
    val ulidFunction = { ulidGenerator.next() }
  }
}
