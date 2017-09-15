package io.github.crabzilla.vertx.entity;

import io.github.crabzilla.core.entity.EntityCommand;
import io.github.crabzilla.core.entity.EntityUnitOfWork;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import static io.github.crabzilla.vertx.helpers.StringHelper.*;


@Slf4j
public class EntityCommandRestVerticle<E> extends AbstractVerticle {

  private final Class<E> aggregateRootClass;
  private final JsonObject config;

  public EntityCommandRestVerticle(@NonNull Class<E> aggregateRootClass, JsonObject config) {
    this.aggregateRootClass = aggregateRootClass;
    this.config = config;
  }

  @Override
  public void start() throws Exception {

    val router = Router.router(vertx);
    router.route(HttpMethod.PUT, "/" + aggregateRootId(aggregateRootClass) + "/commands")
          .handler(putCommandHandler());

    val server = vertx.createHttpServer();
    server.requestHandler(router::accept).listen(config.getInteger("http.port"));

  }

  Handler<RoutingContext> putCommandHandler() {
    return routingContext -> {

      routingContext.request().bodyHandler(buff -> {
        val command = Json.decodeValue(new String(buff.getBytes()), EntityCommand.class);
        val httpResp = routingContext.request().response();
        val options = new DeliveryOptions().setCodecName(EntityCommand.class.getSimpleName());

        vertx.<EntityCommandExecution>eventBus().send(commandHandlerId(aggregateRootClass), command, options, response -> {
          if (!response.succeeded()) {
            log.error("eventbus.sendCommand", response.cause());
            httpResp.setStatusCode(500);
            httpResp.end(response.cause().getMessage());
            return;
          }

          val result = (EntityCommandExecution) response.result().body();
          val resultAsJson = Json.encodePrettily(result);
          log.info("result = {}", resultAsJson);

          if (result.getUnitOfWork() != null && result.getUowSequence() != null) {
            val headers = new CaseInsensitiveHeaders().add("uowSequence", result.getUowSequence()+"");
            val optionsUow = new DeliveryOptions().setCodecName(EntityUnitOfWork.class.getSimpleName())
                    .setHeaders(headers);
            vertx.<String>eventBus().publish(eventsHandlerId("example1"), result.getUnitOfWork(), optionsUow);
            httpResp.setStatusCode(201);
          } else {
            httpResp.setStatusCode(400); // TODO
          }

          httpResp.headers().add("content-type", "application/json");
          httpResp.headers().add("content-length", Integer.toString(resultAsJson.length()));
          httpResp.end(resultAsJson);
        });
      });
    };
  }

}
