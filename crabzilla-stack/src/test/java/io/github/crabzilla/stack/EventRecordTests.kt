package io.github.crabzilla.stack

import io.github.crabzilla.example1.CustomerEvent
import io.github.crabzilla.example1.customerJson
import io.vertx.core.json.JsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class EventRecordTests {

  val id = UUID.fromString("c2aeadc1-d6b5-4df6-82a4-7dec4f1df429")
  val event = CustomerEvent.CustomerRegistered(id, "customer1")
  val eventAsJson = JsonObject(event.toJson(customerJson))
  val eventMetadata = EventMetadata(
    "Customer", AggregateRootId(id),
    EventId(UUID.randomUUID()), CorrelationId(UUID.randomUUID()), CausationId(UUID.randomUUID()),
    1
  )
  val eventRecord = EventRecord(eventMetadata, eventAsJson)
  val eventRecordAsJson = eventRecord.toJsonObject()
  val expectedAsJson =
    JsonObject(
      """{"aggregateName":"Customer",
        "aggregateId":"$id",
        "eventAsjJson":{"type":"CustomerRegistered","id":"$id","name":"customer1"},
        "eventSequence":1,
        "eventId":"${eventRecord.eventMetadata.eventId.id}",
        "causationId":"${eventRecord.eventMetadata.causationId.id}",
        "correlationId":"${eventRecord.eventMetadata.correlationId.id}"
        }
        """
    )

  @Test
  fun toJson() {
//    println(eventRecordAsJson.encodePrettily())
//    println(expectedAsJson.encodePrettily())
    assertThat(eventRecordAsJson).isEqualTo(expectedAsJson)
  }

  @Test
  fun fromJson() {
    assertThat(EventRecord.fromJsonObject(expectedAsJson)).isEqualTo(eventRecord)
  }
}
