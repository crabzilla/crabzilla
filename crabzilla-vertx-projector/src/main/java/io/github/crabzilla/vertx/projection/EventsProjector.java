package io.github.crabzilla.vertx.projection;

import io.github.crabzilla.core.DomainEvent;
import io.github.crabzilla.vertx.ProjectionData;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;

import java.util.List;
import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;

public abstract class EventsProjector<DAO> {

  private static Logger log = getLogger(EventsProjector.class);

  protected final String eventsChannelId;
  protected final Class<DAO> daoClass;
  protected final Jdbi jdbi;

  public EventsProjector(String eventsChannelId, Class<DAO> daoClass, Jdbi jdbi) {
    this.eventsChannelId = eventsChannelId;
    this.daoClass = daoClass;
    this.jdbi = jdbi;
  }

  public void handle(final List<ProjectionData> uowList) {
    log.info("Writing {} units for eventChannel {}", uowList.size(), eventsChannelId);
    final Handle handle = jdbi.open();
    final DAO dao = handle.attach(daoClass);
    try {
      final Stream<TargetIDDomainEventPair> stream = uowList.stream()
              .flatMap(uowData -> uowData.getEvents().stream()
              .map(e -> new TargetIDDomainEventPair(uowData.getTargetId(), e)));
      stream.forEach(tuple2 -> write(dao, tuple2.getId(), tuple2.getEvent()));
      handle.commit();
    } catch (Exception e) {
      log.error("Error with eventChannel " + eventsChannelId, e);
      handle.rollback();
    }
    log.info("Wrote {} units for eventChannel {}", uowList.size(), eventsChannelId);
  }

  public String getEventsChannelId() {
    return eventsChannelId;
  }

  public abstract void write(DAO dao, String targetId, DomainEvent event);

}
