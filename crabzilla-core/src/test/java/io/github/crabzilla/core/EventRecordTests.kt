package io.github.crabzilla.core

import io.github.crabzilla.example1.CustomerEvent
import io.github.crabzilla.example1.customerJson
import io.vertx.core.json.JsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EventRecordTests {

  val event = CustomerEvent.CustomerRegistered(1, "customer1")
  val eventAsJson = JsonObject(customerJson.encodeToString(DOMAIN_EVENT_SERIALIZER, event))
  val eventRecord = EventRecord("Customer", 1, eventAsJson, 1)
  val eventRecordAsJson = eventRecord.toJsonObject()
  val expected =
    """{"aggregateName":"Customer","aggregateId":1,
        |"eventAsjJson":{"type":"CustomerRegistered","id":1,"name":"customer1"},"eventId":1}""".trimMargin()

  @Test
  fun toJson() {
    assertThat(eventRecordAsJson).isEqualTo(JsonObject(expected))
  }

  @Test
  fun fromJson() {
    assertThat(EventRecord.fromJsonObject(JsonObject(expected))).isEqualTo(eventRecord)
  }
}
