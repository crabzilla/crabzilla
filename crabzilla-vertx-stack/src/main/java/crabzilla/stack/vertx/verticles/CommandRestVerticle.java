package crabzilla.stack.vertx.verticles;

import crabzilla.model.AggregateRoot;
import io.vertx.core.AbstractVerticle;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.util.List;

public class CommandRestVerticle<A extends AggregateRoot> extends AbstractVerticle {

  final Class<A> aggregateRootClass;
  final List<Class<?>> commandsClasses;

  public CommandRestVerticle(@NonNull Class<A> aggregateRootClass, @NonNull List<Class<?>> commandsClasses) {
    this.aggregateRootClass = aggregateRootClass;
    this.commandsClasses = commandsClasses;
  }

  @Override
  public void start() throws Exception {

      vertx.createHttpServer()
      .requestHandler(httpRequest -> {
        System.out.println("----> " + LocalDateTime.now() + httpRequest.getHeader("User-Agent"));
        vertx.eventBus().send("hello-input", httpRequest.getParam("name") + " " + LocalDateTime.now(), response -> {
          if (response.succeeded()) {
          /* Send the result from HelloWorldService to the http connection. */
            httpRequest.response().end(response.result().body().toString());
          } else {
            // logger.error("Can't send message to hello service", response.cause());
            httpRequest.response().setStatusCode(500).end(response.cause().getMessage());
          }
        });
      })
      .listen(8080);
    
  }

}
