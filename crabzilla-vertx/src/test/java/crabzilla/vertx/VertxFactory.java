package crabzilla.vertx;

import com.fasterxml.jackson.databind.ObjectMapper;
import crabzilla.model.DomainEvent;
import crabzilla.model.EntityCommand;
import crabzilla.model.EntityId;
import crabzilla.model.EntityUnitOfWork;
import crabzilla.stack.CommandExecution;
import crabzilla.vertx.codecs.JacksonGenericCodec;
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

    vertx.eventBus().registerDefaultCodec(EntityCommand.class,
            new JacksonGenericCodec<>(mapper, EntityCommand.class));

    vertx.eventBus().registerDefaultCodec(DomainEvent.class,
            new JacksonGenericCodec<>(mapper, DomainEvent.class));

    vertx.eventBus().registerDefaultCodec(EntityUnitOfWork.class,
            new JacksonGenericCodec<>(mapper, EntityUnitOfWork.class));

    return vertx;
  }

}
