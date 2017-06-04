package crabzilla.stack.vertx.verticles;

import com.fasterxml.jackson.databind.ObjectMapper;
import crabzilla.model.AggregateRootId;
import crabzilla.model.Command;
import crabzilla.model.Event;
import crabzilla.model.UnitOfWork;
import crabzilla.stack.vertx.CommandExecution;
import crabzilla.stack.vertx.codecs.JacksonGenericCodec;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import lombok.val;

public class VertxFactory {

  Vertx vertx() {

    val vertx = Vertx.vertx();

    val mapper = Json.mapper;
    mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
    mapper.findAndRegisterModules();

    vertx.eventBus().registerDefaultCodec(CommandExecution.class,
            new JacksonGenericCodec<>(mapper, CommandExecution.class));

    vertx.eventBus().registerDefaultCodec(AggregateRootId.class,
            new JacksonGenericCodec<>(mapper, AggregateRootId.class));

    vertx.eventBus().registerDefaultCodec(Command.class,
            new JacksonGenericCodec<>(mapper, Command.class));

    vertx.eventBus().registerDefaultCodec(Event.class,
            new JacksonGenericCodec<>(mapper, Event.class));

    vertx.eventBus().registerDefaultCodec(UnitOfWork.class,
            new JacksonGenericCodec<>(mapper, UnitOfWork.class));

    return vertx;
  }

}
