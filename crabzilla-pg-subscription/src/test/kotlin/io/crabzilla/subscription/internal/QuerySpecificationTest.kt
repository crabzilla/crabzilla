package io.crabzilla.subscription.internal

import io.crabzilla.subscription.internal.QuerySpecification.queryFor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Specifying events query")
internal class QuerySpecificationTest {
  @Test
  fun `all events`() {
    val expected = """
    SELECT streams.id as stream_id, streams.state_type, streams.state_id,
           events.event_type, events.event_payload, events.sequence, events.id as event_id, events.causation_id, events.correlation_id, events.version
      FROM events
      JOIN streams ON streams.id = events.stream_id
     WHERE sequence > (SELECT sequence FROM subscriptions WHERE name = ${'$'}1)
     ORDER BY sequence
     LIMIT ${'$'}2"""
    val sql = queryFor()
    assertEquals(expected, sql)
  }

  @Test
  fun `by state type`() {
    val expected = """
    SELECT streams.id as stream_id, streams.state_type, streams.state_id,
           events.event_type, events.event_payload, events.sequence, events.id as event_id, events.causation_id, events.correlation_id, events.version
      FROM events
      JOIN streams ON streams.id = events.stream_id
     WHERE sequence > (SELECT sequence FROM subscriptions WHERE name = ${'$'}1)
       AND state_type IN ('Customer')
     ORDER BY sequence
     LIMIT ${'$'}2"""
    val sql = queryFor(stateTypes = listOf("Customer"))
    assertEquals(expected.trimIndent(), sql.trimIndent())
  }

  @Test
  fun `by events types`() {
    val expected = """
    SELECT streams.id as stream_id, streams.state_type, streams.state_id,
           events.event_type, events.event_payload, events.sequence, events.id as event_id, events.causation_id, events.correlation_id, events.version
      FROM events
      JOIN streams ON streams.id = events.stream_id
     WHERE sequence > (SELECT sequence FROM subscriptions WHERE name = ${'$'}1)
       AND event_type IN ('CustomerRegistered','CustomerActivated','CustomerDeactivated')
     ORDER BY sequence
     LIMIT ${'$'}2"""
    val sql = queryFor(eventTypes = listOf("CustomerRegistered", "CustomerActivated", "CustomerDeactivated"))
    assertEquals(expected.trimIndent(), sql.trimIndent())
  }

  @Test
  fun `by state and event types`() {
    val expected =
      """
    SELECT streams.id as stream_id, streams.state_type, streams.state_id,
           events.event_type, events.event_payload, events.sequence, events.id as event_id, events.causation_id, events.correlation_id, events.version
      FROM events
      JOIN streams ON streams.id = events.stream_id
     WHERE sequence > (SELECT sequence FROM subscriptions WHERE name = ${'$'}1)
       AND state_type IN ('Customer')
       AND event_type IN ('CustomerRegistered','CustomerActivated','CustomerDeactivated')
     ORDER BY sequence
     LIMIT ${'$'}2"""
    val q =
      queryFor(
        stateTypes = listOf("Customer"),
        eventTypes = listOf("CustomerRegistered", "CustomerActivated", "CustomerDeactivated"),
      )
    assertEquals(expected, q)
  }
}
