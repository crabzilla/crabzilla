package crabzilla.vertx.verticles;

import crabzilla.model.AggregateRoot;
import crabzilla.model.EntityCommand;
import crabzilla.vertx.CommandExecution;
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

import static crabzilla.vertx.util.StringHelper.*;

@Slf4j
public class CommandRestVerticle<A extends AggregateRoot> extends AbstractVerticle {

  final Class<A> aggregateRootClass;

  public CommandRestVerticle(@NonNull Class<A> aggregateRootClass) {
    this.aggregateRootClass = aggregateRootClass;
  }

  @Override
  public void start() throws Exception {

    val router = Router.router(vertx);

    router.route(HttpMethod.PUT, "/" + aggregateRootId(aggregateRootClass) + "/commands")
          .handler(contextHandler());

    val server = vertx.createHttpServer();

    server.requestHandler(router::accept).listen(8080);
  }

  Handler<RoutingContext> contextHandler() {

    return routingContext -> {

      routingContext.request().bodyHandler(buff -> {

        EntityCommand command = Json.decodeValue(new String(buff.getBytes()), EntityCommand.class);
        val httpResp = routingContext.request().response();
        val options = new DeliveryOptions().setCodecName("EntityCommand");

        // create customer command
        vertx.eventBus().<CommandExecution>send(commandHandlerId(aggregateRootClass), command, options, asyncResult -> {

          log.info("Successful create customer test? {}", asyncResult.succeeded());

          if (asyncResult.succeeded()) {

            CommandExecution result = asyncResult.result().body();
            val resultAsJson = Json.encodePrettily(result);
            log.info("commandExecution: {}", result);

            if (CommandExecution.RESULT.SUCCESS.equals(result.getResult())) {
              val headers = new CaseInsensitiveHeaders().add("uowSequence", result.getUowSequence().get().toString());
              val optionsUow = new DeliveryOptions().setCodecName("EntityUnitOfWork").setHeaders(headers);
              vertx.<String>eventBus().publish(eventsHandlerId("example1"), result.getUnitOfWork().get(), optionsUow);
              httpResp.setStatusCode(201);
            } else {
              //  TODO inform more details
              httpResp.setStatusCode(400);
            }

            httpResp.headers().add("content-type", "application/json");
            httpResp.headers().add("content-length", Integer.toString(resultAsJson.length()));
            httpResp.end(resultAsJson);

          } else {
            log.info("Cause: {}", asyncResult.cause());
            log.info("Message: {}", asyncResult.cause().getMessage());
          }

        });




//        vertx.<CommandExecution>eventBus().send(commandHandlerId(aggregateRootClass), command, options, (Handler<AsyncResult<Message<CommandExecution>>>) response -> {
//          if (response.succeeded()) {
//            CommandExecution result = response.result().body();
//            val resultAsJson = Json.encodePrettily(result);
//            log.info("commandExecution: {}", response);
//            if (CommandExecution.RESULT.SUCCESS.equals(result.getResult())) {
//              val headers = new CaseInsensitiveHeaders().add("uowSequence", result.getUowSequence().get().toString());
//              val optionsUow = new DeliveryOptions().setCodecName("EntityUnitOfWork").setHeaders(headers);
//              //vertx.<String>eventBus().publish(eventsHandlerId("example1"), result.getUnitOfWork().get(), optionsUow);
//              httpResp.setStatusCode(201);
//            } else {
//              //  TODO inform more details
//              httpResp.setStatusCode(400);
//            }
//            httpResp.headers().add("content-type", "application/json");
//            httpResp.headers().add("content-length", Integer.toString(resultAsJson.length()));
//            httpResp.end(resultAsJson);
//          } else {
//            httpResp.setStatusCode(500).end(response.cause().getMessage());
//          }
//        });


      });

    };
  }

}
