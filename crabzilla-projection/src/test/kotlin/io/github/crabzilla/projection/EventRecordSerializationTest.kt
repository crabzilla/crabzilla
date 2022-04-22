package io.github.crabzilla.projection

import io.github.crabzilla.TestsFixtures.json
import io.github.crabzilla.core.metadata.EventMetadata
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRegistered
import io.github.crabzilla.stack.EventRecord
import io.github.crabzilla.stack.EventRecord.Companion.fromJsonObject
import io.vertx.core.json.JsonObject
import kotlinx.serialization.encodeToString
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

@DisplayName("Serializing EventRecord")
class EventRecordSerializationTest {

  val id = UUID.fromString("c2aeadc1-d6b5-4df6-82a4-7dec4f1df429")
  val event = CustomerRegistered(id, "customer1") as CustomerEvent
  val eventAsJson = JsonObject(json.encodeToString(event))
  val eventMetadata = EventMetadata(
    "Customer", id, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1
  )
  val eventRecord = EventRecord(eventMetadata, eventAsJson)
  val eventRecordAsJson = eventRecord.toJsonObject()
  val expectedAsJson =
    JsonObject(
      """{"aggregateName":"Customer",
        "aggregateId":"$id",
        "eventAsjJson":{"type":"CustomerRegistered","id":"$id","name":"customer1"},
        "eventSequence":1,
        "eventId":"${eventRecord.eventMetadata.eventId}",
        "causationId":"${eventRecord.eventMetadata.causationId}",
        "correlationId":"${eventRecord.eventMetadata.correlationId}"
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
    assertThat(fromJsonObject(expectedAsJson)).isEqualTo(eventRecord)
  }
}
