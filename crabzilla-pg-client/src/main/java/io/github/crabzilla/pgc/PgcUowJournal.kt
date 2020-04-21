package io.github.crabzilla.pgc

import io.github.crabzilla.EventBusChannels
import io.github.crabzilla.framework.COMMAND_SERIALIZER
import io.github.crabzilla.framework.EVENT_SERIALIZER
import io.github.crabzilla.framework.UnitOfWork
import io.github.crabzilla.internal.UnitOfWorkJournal
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class PgcUowJournal(private val vertx: Vertx, private val pgPool: PgPool, private val json: Json) : UnitOfWorkJournal {

  companion object {
    internal val log = LoggerFactory.getLogger(PgcUowJournal::class.java)
    const val SQL_SELECT_CURRENT_VERSION =
      """ select max(version) as last_version from units_of_work where ar_id = $1 and ar_name = $2 """
    const val SQL_APPEND_UOW =
      """ insert into units_of_work (uow_events, cmd_id, cmd_data, ar_name, ar_id, version) values
         ($1, $2, $3, $4, $5, $6) returning uow_id"""
  }

  override fun append(unitOfWork: UnitOfWork): Future<Long> {
    val promise = Promise.promise<Long>()
    pgPool.begin { event0 ->
      if (event0.failed()) {
        log.error("when starting transaction")
        promise.fail(event0.cause())
        return@begin
      }
      val tx = event0.result()
      val params1 = Tuple.of(unitOfWork.entityId, unitOfWork.entityName)
      tx.preparedQuery(SQL_SELECT_CURRENT_VERSION)
        .execute(params1) { event1 ->
          if (event1.failed()) {
            log.error("when selecting current version")
            promise.fail(event1.cause())
            return@execute
          }
          val currentVersion = event1.result().first()?.getInteger("last_version") ?: 0
          if (currentVersion != unitOfWork.version - 1) {
            val error = "expected version is ${unitOfWork.version - 1} but current version is $currentVersion"
            log.error(error)
            promise.fail(error)
            return@execute
          }
          // if version is OK, then insert
          val cmdAsJsonObject: String = json.stringify(COMMAND_SERIALIZER, unitOfWork.command)
          val eventsListAsJsonObject: String = json.stringify(EVENT_SERIALIZER.list, unitOfWork.events)
          val params2 = Tuple.of(JsonArray(eventsListAsJsonObject), unitOfWork.commandId, JsonObject(cmdAsJsonObject),
            unitOfWork.entityName, unitOfWork.entityId, unitOfWork.version)
          tx.preparedQuery(SQL_APPEND_UOW)
            .execute(params2) { event2 ->
              if (event2.failed()) {
                log.error("Transaction failed", event2.cause())
                promise.fail(event2.cause())
                return@execute
              }
              val insertRows = event2.result().value()
              val uowId = insertRows.first().getLong(0)
              // Commit the transaction
              tx.commit { event3 ->
                if (event3.failed()) {
                  log.error("Transaction failed", event3.cause())
                  promise.fail(event3.cause())
                  return@commit
                }
                log.debug("Transaction succeeded for $uowId")
                val message = JsonObject()
                  .put("uowId", uowId)
                  .put(UnitOfWork.JsonMetadata.ENTITY_NAME, unitOfWork.entityName)
                  .put(UnitOfWork.JsonMetadata.ENTITY_ID, unitOfWork.entityId)
                  .put(UnitOfWork.JsonMetadata.VERSION, unitOfWork.version)
                  .put(UnitOfWork.JsonMetadata.EVENTS, JsonArray(eventsListAsJsonObject))
                if (log.isDebugEnabled) log.debug("will publish message $message")
                vertx.eventBus().publish(EventBusChannels.unitOfWorkChannel, message)
                promise.complete(uowId)
              }
            }
        }
    }
    log.debug("returning {}", promise)
    return promise.future()
  }
}
