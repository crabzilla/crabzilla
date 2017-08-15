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
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.lang.reflect.Field;
import java.util.Set;

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
        val command = Json.decodeValue(new String(buff.getBytes()), EntityCommand.class);
        val httpResp = routingContext.request().response();
        val options = new DeliveryOptions().setCodecName("EntityCommand");
        vertx.<CommandExecution>eventBus().send(commandHandlerId(aggregateRootClass), command, options, response -> {
          if (response.succeeded()) {
            log.info("success commands handler: {}", response);
            val result = (CommandExecution) response.result().body();
            if (CommandExecution.RESULT.SUCCESS.equals(result.getResult())) {
              val headers = new CaseInsensitiveHeaders().add("uowSequence", result.getUowSequence().get().toString());
              val optionsUow = new DeliveryOptions().setCodecName("EntityUnitOfWork").setHeaders(headers);
              vertx.<String>eventBus().publish(eventsHandlerId("example1"), result.getUnitOfWork().get(), optionsUow);
              httpResp.end(response.result().body().toString());
            } else {
              //  TODO inform more details
              httpResp.setStatusCode(500).end(result.getConstraints().get().get(0));
            }
          } else {
            httpResp.setStatusCode(500).end(response.cause().getMessage());
          }
        });
      });

    };
  }

}
