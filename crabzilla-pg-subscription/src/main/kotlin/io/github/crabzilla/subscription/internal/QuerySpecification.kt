package io.github.crabzilla.subscription.internal

internal object QuerySpecification {
  private const val QUERY = """
    SELECT streams.id as stream_id, streams.state_type, streams.state_id,
           events.event_type, events.event_payload, events.sequence, events.id as event_id, events.causation_id, events.correlation_id, events.version
      FROM events
      JOIN streams ON streams.id = events.stream_id
     WHERE sequence > (SELECT sequence FROM subscriptions WHERE name = $1)"""

  fun queryFor(
    stateTypes: List<String> = emptyList(),
    eventTypes: List<String> = emptyList(),
  ): String {
    var sql = QUERY
    if (stateTypes.isNotEmpty()) {
      val list = stateTypes.joinToString(",") { type -> "'$type'" }
      sql =
        sql.plus(
          """
       AND state_type IN ($list)""",
        )
    }
    if (eventTypes.isNotEmpty()) {
      val list = eventTypes.joinToString(",") { type -> "'$type'" }
      sql =
        sql.plus(
          """
       AND event_type IN ($list)""",
        )
    }
    sql =
      sql.plus(
        """
     ORDER BY sequence
     LIMIT $2""",
      )
    return sql
  }
}
