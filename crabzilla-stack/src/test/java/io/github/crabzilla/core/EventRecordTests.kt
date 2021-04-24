package io.github.crabzilla.core

import io.github.crabzilla.example1.CustomerEvent
import io.github.crabzilla.example1.customerJson
import io.github.crabzilla.stack.EventRecord
import io.vertx.core.json.JsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class EventRecordTests {

  val id = UUID.fromString("c2aeadc1-d6b5-4df6-82a4-7dec4f1df429")
  val event = CustomerEvent.CustomerRegistered(id, "customer1")
  val eventAsJson = JsonObject(customerJson.encodeToString(DOMAIN_EVENT_SERIALIZER, event))
  val eventRecord = EventRecord("Customer", id, eventAsJson, 1)
  val eventRecordAsJson = eventRecord.toJsonObject()
  val expected =
    """{"aggregateName":"Customer",
        |"aggregateId":"$id",
        |"eventAsjJson"
        |:{"type":"CustomerRegistered","id":"$id","name":"customer1"},"eventId":1}"""
      .trimMargin()

  @Test
  fun toJson() {
    assertThat(eventRecordAsJson).isEqualTo(JsonObject(expected))
  }

  @Test
  fun fromJson() {
    assertThat(EventRecord.fromJsonObject(JsonObject(expected))).isEqualTo(eventRecord)
  }
}
