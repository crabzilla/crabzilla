package io.github.crabzilla

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.Json

object Crabzilla {

  fun initVertx(vertx: Vertx) {

    Json.mapper
      .registerModule(Jdk8Module())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // TODO test this

    vertx.eventBus().registerDefaultCodec(UnitOfWorkEvents::class.java,
      JacksonGenericCodec(Json.mapper, UnitOfWorkEvents::class.java))

    vertx.eventBus().registerDefaultCodec(Pair::class.java,
      JacksonGenericCodec(Json.mapper, Pair::class.java))

    vertx.eventBus().registerDefaultCodec(Command::class.java,
      JacksonGenericCodec(Json.mapper, Command::class.java))

    vertx.eventBus().registerDefaultCodec(DomainEvent::class.java,
      JacksonGenericCodec(Json.mapper, DomainEvent::class.java))

    vertx.eventBus().registerDefaultCodec(UnitOfWork::class.java,
      JacksonGenericCodec(Json.mapper, UnitOfWork::class.java))

  }

}

typealias CommandHandlerFactory<E> = (CommandMetadata, Command, Snapshot<E>,
                                      Handler<AsyncResult<UnitOfWork>>) -> CommandHandler<E>

typealias Version = Int
