package io.github.crabzilla.vertx.projection;

import io.github.crabzilla.core.entity.EntityUnitOfWork;
import io.github.crabzilla.vertx.entity.EntityCommandHandlerVerticle;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import org.slf4j.Logger;

import static java.util.Collections.singletonList;
import static org.slf4j.LoggerFactory.getLogger;

public class EventsProjectionVerticle<DAO> extends AbstractVerticle {

  private static Logger log = getLogger(EntityCommandHandlerVerticle.class);

  private final EventsProjector<DAO> eventsProjector;
  private final CircuitBreaker circuitBreaker;

  public EventsProjectionVerticle(EventsProjector<DAO> eventsProjector,
                                  CircuitBreaker circuitBreaker) {
    this.eventsProjector = eventsProjector;
    this.circuitBreaker = circuitBreaker;
  }

  @Override
  public void start() {
    vertx.eventBus().consumer(eventsProjector.getEventsChannelId(), msgHandler());
  }

  Handler<Message<EntityUnitOfWork>> msgHandler() {
    return (Message<EntityUnitOfWork> msg) -> vertx.executeBlocking((Future<String> future) -> {
      log.info("Received ProjectionData msg {} ", msg);
      EntityUnitOfWork uow = msg.body();
      Long uowSequence = new Long(msg.headers().get("uowSequence"));
      ProjectionData projectionData = new ProjectionData(uow.getUnitOfWorkId(), uowSequence,
              uow.targetId().stringValue(), uow.getEvents());

      circuitBreaker.fallback(throwable -> {
        log.warn("Fallback for uowHandler ");
        return "fallback";
      })
      .execute(uowHandler(projectionData))
      .setHandler(resultHandler(msg));
    }, resultHandler(msg));
  }

  Handler<Future<String>> uowHandler(final ProjectionData projectionData) {
    return future -> {
      eventsProjector.handle(singletonList(projectionData));
      future.complete("roger that");
    };
  }

  Handler<AsyncResult<String>> resultHandler(final Message<EntityUnitOfWork> msg) {
    return (AsyncResult<String> resultHandler) -> {
      if (!resultHandler.succeeded()) {
        log.error("error cause: {}", resultHandler.cause());
        log.error("error message: {}", resultHandler.cause().getMessage());
        // resultHandler.cause().printStackTrace();
        // TODO customize given the commandResult
        msg.fail(400, resultHandler.cause().getMessage());
      }
      String resp = resultHandler.result();
      log.info("success: {}", resp);
      msg.reply(resp);
    };
  }

}

