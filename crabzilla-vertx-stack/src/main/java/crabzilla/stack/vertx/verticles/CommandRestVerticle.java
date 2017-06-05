package crabzilla.stack.vertx.verticles;

import crabzilla.model.AggregateRoot;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import lombok.NonNull;
import lombok.val;

import java.lang.reflect.Field;
import java.util.Set;

import static crabzilla.stack.util.StringHelper.aggregateRootId;
import static crabzilla.stack.util.StringHelper.commandHandlerId;

public class CommandRestVerticle<A extends AggregateRoot> extends AbstractVerticle {

  final Class<A> aggregateRootClass;

  public CommandRestVerticle(@NonNull Class<A> aggregateRootClass) {
    this.aggregateRootClass = aggregateRootClass;
  }

  @Override
  public void start() throws Exception {

    val router = Router.router(vertx);

    router.route(HttpMethod.PUT, "/" + aggregateRootId(aggregateRootClass) + "/commands").handler(routingContext -> {
      val commandAsJson = routingContext.getBodyAsJson();
      val httpResp = routingContext.request().response();
      vertx.eventBus().send(commandHandlerId(aggregateRootClass), commandAsJson, response -> {
        if (response.succeeded()) {
          /* Send the result from HelloWorldService to the http connection. */
          httpResp.end(response.result().body().toString());
        } else {
          // logger.error("Can't send message to hello service", response.cause());
          httpResp.setStatusCode(500).end(response.cause().getMessage());
        }
      });
    });

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

}
