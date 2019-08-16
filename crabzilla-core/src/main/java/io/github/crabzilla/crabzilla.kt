package io.github.crabzilla

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import io.github.crabzilla.internal.JacksonGenericCodec
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.Json

typealias CommandHandlerFactory<E> = (CommandMetadata, Command, Snapshot<E>,
                                      Handler<AsyncResult<UnitOfWork>>) -> EntityCommandHandler<E>
typealias Version = Int

fun Vertx.initCrabzilla() {
  initCrabzillaFor(this)
}

fun initCrabzillaFor(vertx: Vertx) {

  Json.mapper
    .registerModule(Jdk8Module())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

  vertx.eventBus().registerDefaultCodec(Pair::class.java,
    JacksonGenericCodec(Json.mapper, Pair::class.java))

  vertx.eventBus().registerDefaultCodec(Command::class.java,
    JacksonGenericCodec(Json.mapper, Command::class.java))

  vertx.eventBus().registerDefaultCodec(DomainEvent::class.java,
    JacksonGenericCodec(Json.mapper, DomainEvent::class.java))

  vertx.eventBus().registerDefaultCodec(Snapshot::class.java,
    JacksonGenericCodec(Json.mapper, Snapshot::class.java))

  vertx.eventBus().registerDefaultCodec(RangeOfEvents::class.java,
    JacksonGenericCodec(Json.mapper, RangeOfEvents::class.java))

  vertx.eventBus().registerDefaultCodec(UnitOfWork::class.java,
    JacksonGenericCodec(Json.mapper, UnitOfWork::class.java))

  vertx.eventBus().registerDefaultCodec(UnitOfWorkEvents::class.java,
    JacksonGenericCodec(Json.mapper, UnitOfWorkEvents::class.java))

  vertx.eventBus().registerDefaultCodec(CommandMetadata::class.java,
    JacksonGenericCodec(Json.mapper, CommandMetadata::class.java))

}
