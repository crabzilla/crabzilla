package io.github.crabzilla

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import java.util.*

// schema

interface DomainEvent

interface Command

typealias Version = Int

interface Entity {
  fun eventsOf(vararg event: DomainEvent): List<DomainEvent> {
    return event.asList()
  }
}

// runtime

typealias CommandHandlerFactory<E> = (CommandMetadata, Command, Snapshot<E>,
                                      Handler<AsyncResult<UnitOfWork>>) -> CommandHandler<E>

data class Snapshot<E : Entity>(val state: E, val version: Version)

data class SnapshotData(val version: Version, val events: List<Pair<String, DomainEvent>>)

data class ProjectionData(val uowId: UUID, val uowSequence: Int, val entityId: Int,
                          val events: List<Pair<String, DomainEvent>>) {
  companion object {
    fun fromUnitOfWork(uowSequence: Int, uow: UnitOfWork) : ProjectionData {
      return ProjectionData(uow.unitOfWorkId, uowSequence, uow.entityId, uow.events)
    }
  }
}

// vertx

fun initVertx(vertx: Vertx) {

  Json.mapper
    .registerModule(Jdk8Module())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // TODO test this

  vertx.eventBus().registerDefaultCodec(ProjectionData::class.java,
    JacksonGenericCodec(Json.mapper, ProjectionData::class.java))

  vertx.eventBus().registerDefaultCodec(Pair::class.java,
    JacksonGenericCodec(Json.mapper, Pair::class.java))

  vertx.eventBus().registerDefaultCodec(Command::class.java,
    JacksonGenericCodec(Json.mapper, Command::class.java))

  vertx.eventBus().registerDefaultCodec(DomainEvent::class.java,
    JacksonGenericCodec(Json.mapper, DomainEvent::class.java))

  vertx.eventBus().registerDefaultCodec(UnitOfWork::class.java,
    JacksonGenericCodec(Json.mapper, UnitOfWork::class.java))

}


// deployment

