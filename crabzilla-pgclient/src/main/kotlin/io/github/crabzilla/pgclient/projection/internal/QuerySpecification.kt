package io.github.crabzilla.pgclient.projection.internal

internal object QuerySpecification {

  private const val QUERY = """
        SELECT event_type, state_type, state_id, event_payload, sequence, id, causation_id, correlation_id
          FROM events
        WHERE sequence > (select sequence from projections where name = $1)"""

  fun query(stateTypes: List<String>, eventTypes: List<String>): String {
    var sql = QUERY
    if (stateTypes.isNotEmpty()) {
      val list = stateTypes.joinToString(",") { type -> "'$type'" }
      sql = sql.plus(
        """ 
        AND state_type IN ($list)"""
      )
    }
    if (eventTypes.isNotEmpty()) {
      val list = eventTypes.joinToString(",") { type -> "'$type'" }
      sql = sql.plus(
        """
         AND event_type IN ($list)"""
      )
    }
    sql = sql.plus(
      """
      ORDER BY sequence 
      LIMIT $2"""
    )
    return sql
  }
}
