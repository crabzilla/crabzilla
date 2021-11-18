package io.github.crabzilla.projection

import io.github.crabzilla.core.metadata.EventMetadata
import io.github.crabzilla.core.metadata.Metadata.CausationId
import io.github.crabzilla.core.metadata.Metadata.CorrelationId
import io.github.crabzilla.core.metadata.Metadata.EventId
import io.github.crabzilla.core.metadata.Metadata.StateId
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRegistered
import io.github.crabzilla.example1.example1Json
import io.github.crabzilla.json.KotlinJsonSerDer
import io.github.crabzilla.projection.EventRecord.Companion.fromJsonObject
import io.vertx.core.json.JsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class EventRecordTest {

  val serDer = KotlinJsonSerDer(example1Json)

  val id = UUID.fromString("c2aeadc1-d6b5-4df6-82a4-7dec4f1df429")
  val event = CustomerRegistered(id, "customer1")
  val eventAsJson = JsonObject(serDer.toJson(event))
  val eventMetadata = EventMetadata(
    "Customer", StateId(id),
    EventId(UUID.randomUUID()), CorrelationId(UUID.randomUUID()),
    CausationId(UUID.randomUUID()),
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
    val equalTo = assertThat(fromJsonObject(expectedAsJson)).isEqualTo(eventRecord)
  }
}
