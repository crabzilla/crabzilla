package io.github.crabzilla.vertx.projection;

import io.github.crabzilla.core.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import java.util.List;
import java.util.stream.Stream;

@Slf4j
public abstract class EventProjector<DAO> {

  protected final String eventsChannelId;
  protected final Class<DAO> daoClass;
  protected final Jdbi jdbi;

  protected EventProjector(String eventsChannelId, Class<DAO> daoClass, Jdbi jdbi) {
    this.eventsChannelId = eventsChannelId;
    this.daoClass = daoClass;
    this.jdbi = jdbi;
  }

  public void handle(final List<ProjectionData> uowList) {
    log.info("Writing {} units for eventChannel {}", uowList.size(), eventsChannelId);
    final Handle handle = jdbi.open();
    final DAO dao = handle.attach(daoClass);
    try {
      final Stream<EventProjectorTuple> stream = uowList.stream()
              .flatMap(uowData -> uowData.getEvents().stream()
                      .map(e -> new EventProjectorTuple(uowData.getTargetId(), e)));
      stream.forEach(tuple2 -> write(dao, tuple2.getId(), tuple2.getEvent()));
      handle.commit();
    } catch (Exception e) {
      log.error("Error with eventChannel " + eventsChannelId, e);
      handle.rollback();
    }
    log.info("Wrote {} units for eventChannel {}", uowList.size(), eventsChannelId);
  }

  public abstract void write(DAO dao, String targetId, DomainEvent event);

}
