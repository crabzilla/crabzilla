package io.github.crabzilla.pgclient.projection.internal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Specifying events query")
internal class QuerySpecificationTest {

  @Test
  fun `all events`() {
    val expected = """
        SELECT event_type, state_type, state_id, event_payload, sequence, id, causation_id, correlation_id
          FROM events
        WHERE sequence > (select sequence from projections where name = $1)
      ORDER BY sequence 
      LIMIT $2"""
    val q = QuerySpecification.query(emptyList(), emptyList())
    assertEquals(q, expected)
  }

  @Test
  fun `by aggregate root`() {
    val expected = """
        SELECT event_type, state_type, state_id, event_payload, sequence, id, causation_id, correlation_id
          FROM events
        WHERE sequence > (select sequence from projections where name = $1) 
        AND state_type IN ('Customer')
      ORDER BY sequence 
      LIMIT $2"""
    val q = QuerySpecification.query(listOf("Customer"), emptyList())
    assertEquals(q, expected)
  }

  @Test
  fun `by events`() {
    val expected = """
        SELECT event_type, state_type, state_id, event_payload, sequence, id, causation_id, correlation_id
          FROM events
        WHERE sequence > (select sequence from projections where name = $1)
         AND event_type IN ('CustomerRegistered','CustomerActivated','CustomerDeactivated')
      ORDER BY sequence 
      LIMIT $2"""
    val q = QuerySpecification.query(
      emptyList(),
      listOf("CustomerRegistered", "CustomerActivated", "CustomerDeactivated")
    )
    assertEquals(q, expected)
  }

  @Test
  fun `by aggregate root and events`() {
    val expected = """
        SELECT event_type, state_type, state_id, event_payload, sequence, id, causation_id, correlation_id
          FROM events
        WHERE sequence > (select sequence from projections where name = $1) 
        AND state_type IN ('Customer')
         AND event_type IN ('CustomerRegistered','CustomerActivated','CustomerDeactivated')
      ORDER BY sequence 
      LIMIT $2"""
    val q = QuerySpecification.query(
      listOf("Customer"),
      listOf("CustomerRegistered", "CustomerActivated", "CustomerDeactivated")
    )
    assertEquals(q, expected)
  }
}
