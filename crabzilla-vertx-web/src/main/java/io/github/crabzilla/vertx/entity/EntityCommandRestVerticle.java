package io.github.crabzilla.vertx.entity;

import io.github.crabzilla.core.entity.EntityCommand;
import io.github.crabzilla.core.entity.EntityUnitOfWork;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.UUID;

import static io.github.crabzilla.vertx.helpers.StringHelper.*;


@Slf4j
public class EntityCommandRestVerticle<E> extends AbstractVerticle {

  private final Class<E> aggregateRootClass;
  private final JsonObject config;
  private final EntityUnitOfWorkRepository entityUowRepo;

  public EntityCommandRestVerticle(@NonNull Class<E> aggregateRootClass,
                                   @NonNull JsonObject config,
                                   @NonNull EntityUnitOfWorkRepository entityUowRepo) {
    this.aggregateRootClass = aggregateRootClass;
    this.config = config;
    this.entityUowRepo = entityUowRepo;
  }

  @Override
  public void start() throws Exception {

    val router = Router.router(vertx);

    router.route().handler(BodyHandler.create());

    router.post("/" + aggregateId(aggregateRootClass) + "/commands")
            .handler(this::postCommandHandler);

    router.get("/" + aggregateId(aggregateRootClass) + "/commands/:cmdID")
            .handler(this::getUowByCmdId);

    val server = vertx.createHttpServer();

    server.requestHandler(router::accept)
            .listen(config.getInteger("http.port"));

  }

  void postCommandHandler(RoutingContext routingContext) {

    final HttpServerResponse httpResp = routingContext.response();
    final String commandStr = routingContext.getBodyAsString();

    log.info("command=:\n" + commandStr);

    final EntityCommand command = Json.decodeValue(commandStr, EntityCommand.class);

    if (command == null) {
      sendError(400, httpResp);
      return ;
    }

    final String cmdID = command.getCommandId().toString();

    final Future<EntityUnitOfWork> uowFuture = Future.future();
    entityUowRepo.getUowByCmdId(UUID.fromString(cmdID), uowFuture);

    uowFuture.setHandler(uowResult -> {
      if (uowResult.failed()) {
        sendError(500, httpResp);
        return;
      }

      if (uowResult.result() != null) {
        httpResp.setStatusCode(201);
        val location = routingContext.request().absoluteURI() + "/"
                + uowResult.result().getCommand().getCommandId().toString();
        httpResp.headers().add("Location", location);
        val resultAsJson = Json.encode(uowResult.result());
        httpResp.headers().add("Content-Type", "application/json");
        httpResp.end(resultAsJson);
        return ;
      }

      val options = new DeliveryOptions().setCodecName(EntityCommand.class.getSimpleName());

      vertx.<EntityCommandExecution>eventBus().send(commandHandlerId(aggregateRootClass), command, options, response -> {
        if (!response.succeeded()) {
          log.error("eventbus.handleCommand", response.cause());
          httpResp.setStatusCode(500);
          httpResp.end(response.cause().getMessage());
          return;
        }

        val result = (EntityCommandExecution) response.result().body();
        log.info("result = {}", result);

        if (result.getUnitOfWork() != null && result.getUowSequence() != null) {
          val headers = new CaseInsensitiveHeaders().add("uowSequence", result.getUowSequence()+"");
          val optionsUow = new DeliveryOptions().setCodecName(EntityUnitOfWork.class.getSimpleName())
                  .setHeaders(headers);
          vertx.<String>eventBus().publish(eventsHandlerId("example1"), result.getUnitOfWork(), optionsUow);
          httpResp.setStatusCode(201);
          val location = routingContext.request().absoluteURI() + "/" + result.getUnitOfWork()
                  .getCommand().getCommandId().toString();
          httpResp.headers().add("Location", location);
        } else {
          httpResp.setStatusCode(400); // TODO
        }
        httpResp.end();
      });

    });

  }

  void getUowByCmdId(RoutingContext routingContext) {

    final HttpServerResponse httpResp = routingContext.response();
    final String cmdID = routingContext.request().getParam("cmdID");

    if (cmdID == null) {
      sendError(400, httpResp);
      return ;
    }

    final Future<EntityUnitOfWork> uowFuture = Future.future();
    entityUowRepo.getUowByCmdId(UUID.fromString(cmdID), uowFuture);

    uowFuture.setHandler(uowResult -> {
      if (uowResult.failed()) {
        sendError(500, httpResp);
      }

      if (uowResult.result() == null) {
        sendError(404, httpResp);
      } else {
        val resultAsJson = Json.encode(uowResult.result());
        httpResp.headers().add("Content-Type", "application/json");
        httpResp.end(resultAsJson);
      }

    });

  }

  private void sendError(int statusCode, HttpServerResponse response) {
    response.setStatusCode(statusCode).end();
  }

}
