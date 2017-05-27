package crabzilla.stack.vertx.verticles;

import crabzilla.model.EventsProjector;
import crabzilla.model.UnitOfWork;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

import static crabzilla.stack.util.StringHelper.eventsHandlerId;

@Slf4j
public class EventsProjectionVerticle extends AbstractVerticle {

  final Vertx vertx;
  final EventsProjector eventsProjector;

  @Inject
  public EventsProjectionVerticle(@NonNull Vertx vertx, EventsProjector eventsProjector) {
    this.vertx = vertx;
    this.eventsProjector = eventsProjector;
  }

  @Override
  public void start() throws Exception {

    vertx.eventBus().consumer(eventsHandlerId("example1"), (Message<UnitOfWork> uow) -> {

      log.info("Received an UnitOfWork {} ", uow);

//      eventsProjector.handle();

    }).handler(event -> event.reply("ok, events were projected to db"));

  }
}
