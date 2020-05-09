// package io.github.crabzilla.pgc.example1
//
// import io.github.crabzilla.core.DomainEvent
// import io.github.crabzilla.pgc.jooq.PgcJooqEventProjector
// import io.vertx.core.Future
// import io.vertx.core.Future.failedFuture
// import io.vertx.sqlclient.Transaction
// import io.vertx.sqlclient.Tuple
// import org.slf4j.LoggerFactory
//
// class BadJooqEventProjector : PgcJooqEventProjector {
//
//  private val log = LoggerFactory.getLogger(BadJooqEventProjector::class.java.name)
//
//  override fun handle(pgTx: Transaction, targetId: Int, event: DomainEvent): Future<Void> {
//
//    log.info("event {} ", event)
//
//    return when (event) {
//      is CustomerCreated -> {
//        val query = "INSERT INTO XXXXXX (id, name, is_active) VALUES ($1, $2, $3)"
//        val tuple = Tuple.of(targetId, event.name, false)
//        executePreparedQuery(pgTx, query, tuple)
//      }
//      is CustomerActivated -> {
//        val query = "UPDATE XXX SET is_active = true WHERE id = $1"
//        val tuple = Tuple.of(targetId)
//        executePreparedQuery(pgTx, query, tuple)
//      }
//      is CustomerDeactivated -> {
//        val query = "UPDATE XX SET is_active = false WHERE id = $1"
//        val tuple = Tuple.of(targetId)
//        executePreparedQuery(pgTx, query, tuple)
//      }
//      else -> {
//        failedFuture("${event.javaClass.simpleName} does not have any event projector handler")
//      }
//    }
//  }
// }
