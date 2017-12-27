package io.github.crabzilla.vertx.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.crabzilla.core.DomainEvent;
import io.github.crabzilla.core.entity.EntityCommand;
import io.github.crabzilla.core.entity.EntityId;
import io.github.crabzilla.core.entity.EntityUnitOfWork;
import io.github.crabzilla.vertx.codecs.JacksonGenericCodec;
import io.github.crabzilla.vertx.entity.EntityCommandExecution;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;

public class VertxFactory {

  public Vertx vertx() {

    Vertx vertx = Vertx.vertx();

    ObjectMapper mapper = Json.mapper;
    mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
    mapper.findAndRegisterModules();

    vertx.eventBus().registerDefaultCodec(EntityCommandExecution.class,
            new JacksonGenericCodec<>(mapper, EntityCommandExecution.class));

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
