 package io.github.crabzilla.pgc.command
//
// import io.github.crabzilla.core.command.COMMAND_SERIALIZER
// import io.github.crabzilla.core.command.Command
// import io.github.crabzilla.core.command.DOMAIN_EVENT_SERIALIZER
// import io.github.crabzilla.core.command.DomainEvent
// import io.github.crabzilla.core.command.EventBusChannels
// import io.github.crabzilla.core.command.UnitOfWork
// import io.github.crabzilla.core.command.UnitOfWorkJournal
// import io.github.crabzilla.core.command.UnitOfWorkPublisher
// import io.vertx.core.Future
// import io.vertx.core.Promise
// import io.vertx.core.Vertx
// import io.vertx.core.json.JsonArray
// import io.vertx.core.json.JsonObject
// import io.vertx.pgclient.PgPool
// import io.vertx.sqlclient.Tuple
// import kotlinx.serialization.builtins.list
// import kotlinx.serialization.json.Json
// import org.slf4j.LoggerFactory
//
// class PgcUowJournal(
//  private val writeModelDb: PgPool,
//  private val json: Json,
//  private val uowPublisher: UnitOfWorkPublisher
// ) : UnitOfWorkJournal {
//
//  companion object {
//    private val log = LoggerFactory.getLogger(PgcUowJournal::class.java)
//    const val SQL_SELECT_CURRENT_VERSION =
//      """ select max(version) as last_version from crabz_units_of_work where ar_id = $1 and ar_name = $2 """
//    const val SQL_APPEND_UOW =
//      """ insert into crabz_units_of_work (uow_events, cmd_id, cmd_data, ar_name, ar_id, version) values
//         ($1, $2, $3, $4, $5, $6) returning uow_id"""
//    const val SQL_APPEND_CMD =
//      """ insert into crabz_commands (uow_id, cmd_id, cmd_data) values
//         ($1, $2, $3)"""
//  }
//
//  override fun append(unitOfWork: UnitOfWork): Future<Long> {
//    val promise = Promise.promise<Long>()
//    writeModelDb.begin { event0 ->
//      if (event0.failed()) {
//        log.error("when starting transaction")
//        promise.fail(event0.cause())
//        return@begin
//      }
//      val tx = event0.result()
//      val params1 = Tuple.of(unitOfWork.aggregateRootId, unitOfWork.entityName)
//      tx.preparedQuery(SQL_SELECT_CURRENT_VERSION)
//        .execute(params1) { event1 ->
//          if (event1.failed()) {
//            log.error("when selecting current version")
//            promise.fail(event1.cause())
//            return@execute
//          }
//          val currentVersion = event1.result().first()?.getInteger("last_version") ?: 0
//          if (currentVersion != unitOfWork.version - 1) {
//            val error = "expected version is ${unitOfWork.version - 1} but current version is $currentVersion"
//            log.error(error)
//            promise.fail(error)
//            return@execute
//          }
//          // if version is OK, then insert
//          val cmdAsJsonObject: String = json.encodeToString<Command>(unitOfWork.command)
//          val eventsListAsJsonObject: String = json.encodeToString<List<DomainEvent>>(unitOfWork.events)
//          val params2 = Tuple.of(JsonArray(eventsListAsJsonObject), unitOfWork.commandId, JsonObject(cmdAsJsonObject),
//            unitOfWork.entityName, unitOfWork.aggregateRootId, unitOfWork.version)
//          tx.preparedQuery(SQL_APPEND_UOW)
//            .execute(params2) { event2 ->
//              if (event2.failed()) {
//                log.error("Transaction failed", event2.cause())
//                promise.fail(event2.cause())
//                return@execute
//              }
//              val insertRows = event2.result().value()
//              val uowId = insertRows.first().getLong(0)
//              val params3 = Tuple.of(uowId, unitOfWork.commandId, JsonObject(cmdAsJsonObject))
//              tx.preparedQuery(SQL_APPEND_CMD)
//                .execute(params3) { event3 ->
//                  // Commit the transaction
//                  tx.commit { event4 ->
//                    if (event4.failed()) {
//                      log.error("Transaction failed", event4.cause())
//                      promise.fail(event4.cause())
//                      return@commit
//                    }
//                    log.debug("Transaction succeeded for $uowId")
//                    val message = JsonObject()
//                      .put("uowId", uowId)
//                      .put(UnitOfWork.JsonMetadata.ENTITY_NAME, unitOfWork.entityName)
//                      .put(UnitOfWork.JsonMetadata.ENTITY_ID, unitOfWork.aggregateRootId)
//                      .put(UnitOfWork.JsonMetadata.VERSION, unitOfWork.version)
//                      .put(UnitOfWork.JsonMetadata.EVENTS, JsonArray(eventsListAsJsonObject))
//                    uowPublisher.publish(message)
//                      .onFailure { err -> log.error("publishing events", err); promise.fail(err) }
//                      .onSuccess { result ->
//                        log.info("Message published to ${unitOfWork.entityName} result $result")
//                        promise.complete(uowId)
//                      }
//                  }
//                }
//              }
//        }
//    }
//    return promise.future()
//  }
//
//  class FullPayloadPublisher(private val vertx: Vertx) : UnitOfWorkPublisher {
//    override fun publish(events: JsonObject): Future<Long> {
//      vertx.eventBus()
//        .publish(EventBusChannels.aggregateRootChannel(events.getString(UnitOfWork.JsonMetadata.ENTITY_NAME)), events)
//      return Future.succeededFuture(1)
//    }
//  }
// }
