package io.github.crabzilla.vertx.verticles;

import io.github.crabzilla.model.EntityCommand;
import io.github.crabzilla.model.EntityUnitOfWork;
import io.github.crabzilla.stack.CommandExecution;
import io.github.crabzilla.stack.StringHelper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import static io.github.crabzilla.stack.StringHelper.*;


@Slf4j
public class CommandRestVerticle<E> extends AbstractVerticle {

  final Class<E> aggregateRootClass;

  public CommandRestVerticle(@NonNull Class<E> aggregateRootClass) {
    this.aggregateRootClass = aggregateRootClass;
  }

  @Override
  public void start() throws Exception {
    val router = Router.router(vertx);
    router.route(HttpMethod.PUT, "/" + aggregateRootId(aggregateRootClass) + "/commands")
          .handler(contextHandler());
    val server = vertx.createHttpServer();
    server.requestHandler(router::accept).listen(config().getInteger("http.port", 8080));
  }

  Handler<RoutingContext> contextHandler() {
    return routingContext -> {

      routingContext.request().bodyHandler(buff -> {
        val command = Json.decodeValue(new String(buff.getBytes()), EntityCommand.class);
        val httpResp = routingContext.request().response();
        val options = new DeliveryOptions().setCodecName("EntityCommand");

        vertx.<CommandExecution>eventBus().send(commandHandlerId(aggregateRootClass), command, options, response -> {
          if (response.succeeded()) {
            val result = (CommandExecution) response.result().body();
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
          } else {
            response.cause().printStackTrace();
            httpResp.setStatusCode(500);
            httpResp.end(response.cause().getMessage());
          }
        });
      });
    };
  }

}
