package crabzilla.stack.vertx;

import crabzilla.model.AggregateRoot;
import crabzilla.model.Command;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
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

import static crabzilla.stack.vertx.util.StringHelper.*;

@Slf4j
public class CommandRestVerticle<A extends AggregateRoot> extends AbstractVerticle {

  final Vertx vertx;
  final Class<A> aggregateRootClass;

  public CommandRestVerticle(Vertx vertx, @NonNull Class<A> aggregateRootClass) {
    this.vertx = vertx;
    this.aggregateRootClass = aggregateRootClass;
  }

  @Override
  public void start() throws Exception {

    val router = Router.router(vertx);

    router.route(HttpMethod.PUT, "/" + aggregateRootId(aggregateRootClass) + "/commands")
          .handler(contextHandler());

    // Getting the routes
    for (Route r : router.getRoutes()) {
      // Path is public, but methods are not. We change that
      Field f = r.getClass().getDeclaredField("methods");
      f.setAccessible(true);
      Set<HttpMethod> methods = (Set<HttpMethod>) f.get(r);
      System.out.println(methods.toString() + r.getPath());
    }

    val server = vertx.createHttpServer();

    server.requestHandler(router::accept).listen(8080);
  }

  Handler<RoutingContext> contextHandler() {
    return routingContext -> {
      routingContext.request().bodyHandler(buff -> {
        val command = Json.decodeValue(new String(buff.getBytes()), Command.class);
        val httpResp = routingContext.request().response();
        val options = new DeliveryOptions().setCodecName("Command");
        vertx.<CommandExecution>eventBus().send(commandHandlerId(aggregateRootClass), command, options, response -> {
          if (response.succeeded()) {
            log.info("success commands handler: {}", response);
            val result = (CommandExecution) response.result().body();
            if (CommandExecution.RESULT.SUCCESS.equals(result.getResult())) {
              val headers = new CaseInsensitiveHeaders().add("uowSequence", result.getUowSequence().get().toString());
              val optionsUow = new DeliveryOptions().setCodecName("UnitOfWork").setHeaders(headers);
              vertx.<String>eventBus().send(eventsHandlerId("example1"), result.getUnitOfWork().get(), optionsUow, resp -> {
                if (resp.succeeded()) {
                  log.info("success events handler: {}", resp);
                } else {
                  log.error("error cause: {}", resp.cause());
                  log.error("error message: {}", resp.cause().getMessage());
                }
              });
              httpResp.end(response.result().body().toString());
            } else {
              //  TODO inform more details
              httpResp.setStatusCode(500).end(result.getConstraints().get().get(0));
            }
          } else {
            //  TODO inform more details
            httpResp.setStatusCode(500).end(response.cause().getMessage());
          }
        });
      });

    };
  }

}
