package io.github.crabzilla.vertx.verticles;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import io.github.crabzilla.model.EntityUnitOfWork;
import io.github.crabzilla.stack.EventProjector;
import io.github.crabzilla.stack.ProjectionData;
import io.github.crabzilla.stack.StringHelper;

import static java.util.Collections.singletonList;

@Slf4j
public class EventsProjectionVerticle<DAO> extends AbstractVerticle {

  final EventProjector<DAO> eventProjector;
  final CircuitBreaker circuitBreaker;

  public EventsProjectionVerticle(@NonNull EventProjector<DAO> eventProjector,
                                  @NonNull CircuitBreaker circuitBreaker) {
    this.eventProjector = eventProjector;
    this.circuitBreaker = circuitBreaker;
  }

  @Override
  public void start() throws Exception {
    vertx.eventBus().consumer(StringHelper.eventsHandlerId("example1"), msgHandler());
  }

  Handler<Message<EntityUnitOfWork>> msgHandler() {
    return (Message<EntityUnitOfWork> msg) -> {

      vertx.executeBlocking((Future<String> future) -> {
        log.info("Received ProjectionData msg {} ", msg);
        val uow = msg.body();
        val uowSequence = new Long(msg.headers().get("uowSequence"));
        val projectionData =
                new ProjectionData(uow.getUnitOfWorkId(), uowSequence,
                        uow.targetId().stringValue(), uow.getEvents());

        circuitBreaker.fallback(throwable -> {
          log.warn("Fallback for uowHandler ");
          return "fallback";
        })
        .execute(uowHandler(projectionData))
        .setHandler(resultHandler(msg));
      }, resultHandler(msg));
    };
  }

  Handler<Future<String>> uowHandler(final ProjectionData projectionData) {
    return future -> {
      eventProjector.handle(singletonList(projectionData));
      future.complete("roger that");
    };
  }

  Handler<AsyncResult<String>> resultHandler(final Message<EntityUnitOfWork> msg) {
    return (AsyncResult<String> resultHandler) -> {
      if (resultHandler.succeeded()) {
        val resp = resultHandler.result();
        log.info("success: {}", resp);
        msg.reply(resp);
      } else {
        log.error("error cause: {}", resultHandler.cause());
        log.error("error message: {}", resultHandler.cause().getMessage());
        resultHandler.cause().printStackTrace();
        // TODO customize conform commandResult
        msg.fail(400, resultHandler.cause().getMessage());
      }
    };
  }

}

