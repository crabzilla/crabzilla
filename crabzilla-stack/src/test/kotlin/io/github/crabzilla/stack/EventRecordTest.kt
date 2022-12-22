package io.github.crabzilla.stack

import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRegistered
import io.github.crabzilla.example1.customer.CustomerJsonObjectSerDer
import io.github.crabzilla.example1.customer.ulidFunction
import io.github.crabzilla.stack.EventRecord.Companion.fromJsonObject
import io.github.crabzilla.stack.EventRecord.Companion.toJsonArray
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Serializing EventRecord")
internal class EventRecordTest {

  private val id: String = "c2aeadc1-d6b5-4df6-82a4-7dec4f1df429"
  private val event = CustomerRegistered(id, "John") as CustomerEvent
  private val eventAsJson = CustomerJsonObjectSerDer().eventToJson(event)
  private val eventMetadata = EventMetadata(
    stateType = "Customer",
    stateId = id,
    eventId = ulidFunction.invoke(),
    correlationId = ulidFunction.invoke(),
    causationId = ulidFunction.invoke(),
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

  @Test
  fun listToJsonArray() {
    val eventRecord = EventRecord(eventMetadata, eventAsJson)
    val list = listOf(eventRecord)
    val eventRecordAsJson = eventRecord.toJsonObject()
    val jsonArray = list.toJsonArray()
    assertThat(eventRecordAsJson).isEqualTo(jsonArray.first())
  }
}
