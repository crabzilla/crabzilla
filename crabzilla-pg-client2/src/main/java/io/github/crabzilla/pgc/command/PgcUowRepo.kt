 package io.github.crabzilla.pgc.command
//
// import io.github.crabzilla.core.command.COMMAND_SERIALIZER
// import io.github.crabzilla.core.command.Command
// import io.github.crabzilla.core.command.DOMAIN_EVENT_SERIALIZER
// import io.github.crabzilla.core.command.DomainEvent
// import io.github.crabzilla.core.command.UnitOfWork
// import io.github.crabzilla.core.command.UnitOfWorkRepository
// import io.vertx.core.Future
// import io.vertx.core.Promise
// import io.vertx.core.json.JsonArray
// import io.vertx.core.json.JsonObject
// import io.vertx.pgclient.PgPool
// import io.vertx.sqlclient.Tuple
// import java.util.ArrayList
// import java.util.UUID
// import kotlinx.serialization.builtins.list
// import kotlinx.serialization.json.Json
// import org.slf4j.LoggerFactory
//
// class PgcUowRepo(private val writeModelDb: PgPool, private val json: Json) : UnitOfWorkRepository {
//
//  companion object {
//
//    internal val log = LoggerFactory.getLogger(PgcUowRepo::class.java)
//
//    private const val UOW_ID = "uow_id"
//    private const val CMD_ID = "cmd_id"
//    private const val AR_ID = "ar_id"
//    private const val AR_NAME = "ar_name"
//    private const val VERSION = "version"
//
//    private const val SELECT_LAST_UOW_ID = "select max(uow_id) from crabz_units_of_work"
//    private const val SELECT_FIELDS = "select uow_id, uow_events, cmd_id, cmd_data, ar_id, ar_name, version"
//    private const val SELECT_UOW_BY_CMD_ID = "$SELECT_FIELDS from crabz_units_of_work where cmd_id = $1"
//    private const val SELECT_UOW_BY_UOW_ID = "$SELECT_FIELDS from crabz_units_of_work where uow_id = $1"
//    private const val SELECT_UOW_BY_ENTITY_ID = "$SELECT_FIELDS from crabz_units_of_work where ar_id = $1 order by version"
//  }
//
//  override fun selectLastUowId(): Future<Long> {
//    val promise = Promise.promise<Long>()
//    writeModelDb
//      .preparedQuery(SELECT_LAST_UOW_ID)
//      .execute { event ->
//        if (event.failed()) {
//          promise.fail(event.cause())
//          return@execute
//        }
//        val result = event.result()
//        promise.complete(if (result == null || result.size() == 0) 0 else result.first().getLong(0))
//      }
//    return promise.future()
//  }
//
//  override fun getUowByCmdId(cmdId: UUID): Future<Pair<UnitOfWork, Long>?> {
//    val promise = Promise.promise<Pair<UnitOfWork, Long>?>()
//    val params = Tuple.of(cmdId)
//    writeModelDb.preparedQuery(SELECT_UOW_BY_CMD_ID)
//      .execute(params) { ar ->
//      if (ar.failed()) {
//        promise.fail(ar.cause())
//        return@execute
//      }
//      val rows = ar.result()
//      if (rows.size() == 0) {
//        promise.complete(null)
//        return@execute
//      }
//      val row = rows.first()
//      val commandAsJson: JsonObject = row.get(JsonObject::class.java, 3)
//      val command: Command = json.parse(COMMAND_SERIALIZER, commandAsJson.encode())
//      val eventsAsJsonArray: JsonArray = row.get(JsonArray::class.java, 1)
//      val events: List<DomainEvent> = json.parse(DOMAIN_EVENT_SERIALIZER.list, eventsAsJsonArray.encode())
//      val uowId = row.getLong(UOW_ID)
//      val uow = UnitOfWork(row.getString(AR_NAME), row.getInteger(AR_ID), row.getUUID(CMD_ID), command,
//        row.getInteger(VERSION)!!, events)
//      promise.complete(Pair(uow, uowId))
//    }
//    return promise.future()
//  }
//
//  override fun getUowByUowId(uowId: Long): Future<UnitOfWork?> {
//    val promise = Promise.promise<UnitOfWork?>()
//    val params = Tuple.of(uowId)
//    writeModelDb.preparedQuery(SELECT_UOW_BY_UOW_ID)
//      .execute(params) { ar ->
//      if (ar.failed()) {
//        promise.fail(ar.cause())
//        return@execute
//      }
//      val rows = ar.result()
//      if (rows.size() == 0) {
//        promise.complete(null)
//        return@execute
//      }
//      val row = rows.first()
//      val commandAsJson: JsonObject = row.get(JsonObject::class.java, 3)
//      val command: Command = json.parse(COMMAND_SERIALIZER, commandAsJson.encode())
//      val eventsAsJson: JsonArray = row.get(JsonArray::class.java, 1)
//      val events: List<DomainEvent> = json.parse(DOMAIN_EVENT_SERIALIZER.list, eventsAsJson.encode())
//      val uow = UnitOfWork(row.getString(AR_NAME), row.getInteger(AR_ID), row.getUUID(CMD_ID), command,
//        row.getInteger(VERSION)!!, events)
//      promise.complete(uow)
//    }
//    return promise.future()
//  }
//
//  // TODO replace with a stream and a consumer impl
//  override fun selectByAggregateRootId(id: Int): Future<List<UnitOfWork>> {
//    val promise = Promise.promise<List<UnitOfWork>>()
//    val params = Tuple.of(id)
//    writeModelDb.preparedQuery(SELECT_UOW_BY_ENTITY_ID)
//      .execute(params) { ar ->
//      if (ar.failed()) {
//        promise.fail(ar.cause())
//        return@execute
//      }
//      val result = ArrayList<UnitOfWork>()
//      val rows = ar.result()
//      for (row in rows) {
//        val commandAsJson: JsonObject = row.get(JsonObject::class.java, 3)
//        val command: Command = json.parse(COMMAND_SERIALIZER, commandAsJson.encode())
//        val eventsAsJson: JsonArray = row.get(JsonArray::class.java, 1)
//        val events: List<DomainEvent> = json.parse(DOMAIN_EVENT_SERIALIZER.list, eventsAsJson.encode())
//        val uow = UnitOfWork(row.getString(AR_NAME), row.getInteger(AR_ID), row.getUUID(CMD_ID), command,
//          row.getInteger(VERSION)!!, events)
//        result.add(uow)
//      }
//      promise.complete(result)
//    }
//    return promise.future()
//  }
// }
