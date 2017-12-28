package io.github.crabzilla.vertx.helpers;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.github.crabzilla.core.DomainEvent;
import io.github.crabzilla.core.entity.EntityCommand;
import io.github.crabzilla.core.entity.EntityId;
import io.github.crabzilla.core.entity.EntityUnitOfWork;
import io.github.crabzilla.vertx.EntityCommandExecution;
import io.github.crabzilla.vertx.codecs.JacksonGenericCodec;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;

public class VertxFactory {

  public Vertx vertx() {

    Vertx vertx = Vertx.vertx();

    Json.mapper.registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .registerModule(new KotlinModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    vertx.eventBus().registerDefaultCodec(EntityCommandExecution.class,
            new JacksonGenericCodec<>(Json.mapper, EntityCommandExecution.class));

    vertx.eventBus().registerDefaultCodec(EntityId.class,
            new JacksonGenericCodec<>(Json.mapper, EntityId.class));

    vertx.eventBus().registerDefaultCodec(EntityCommand.class,
            new JacksonGenericCodec<>(Json.mapper, EntityCommand.class));

    vertx.eventBus().registerDefaultCodec(DomainEvent.class,
            new JacksonGenericCodec<>(Json.mapper, DomainEvent.class));

    vertx.eventBus().registerDefaultCodec(EntityUnitOfWork.class,
            new JacksonGenericCodec<>(Json.mapper, EntityUnitOfWork.class));

    return vertx;
  }

}
