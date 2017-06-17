package crabzilla.example1.extra;


import com.fasterxml.jackson.databind.ObjectMapper;
import crabzilla.model.Command;
import crabzilla.model.EntityId;
import crabzilla.model.Event;
import crabzilla.model.UnitOfWork;
import crabzilla.stack.vertx.CommandExecution;
import crabzilla.stack.vertx.codecs.JacksonGenericCodec;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import lombok.val;

public class VertxFactory {

  public Vertx vertx() {

    val vertx = Vertx.vertx();

    val mapper = Json.mapper;
    mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
    mapper.findAndRegisterModules();

    vertx.eventBus().registerDefaultCodec(CommandExecution.class,
            new JacksonGenericCodec<>(mapper, CommandExecution.class));

    vertx.eventBus().registerDefaultCodec(EntityId.class,
            new JacksonGenericCodec<>(mapper, EntityId.class));

    vertx.eventBus().registerDefaultCodec(Command.class,
            new JacksonGenericCodec<>(mapper, Command.class));

    vertx.eventBus().registerDefaultCodec(Event.class,
            new JacksonGenericCodec<>(mapper, Event.class));

    vertx.eventBus().registerDefaultCodec(UnitOfWork.class,
            new JacksonGenericCodec<>(mapper, UnitOfWork.class));

    return vertx;
  }

}
