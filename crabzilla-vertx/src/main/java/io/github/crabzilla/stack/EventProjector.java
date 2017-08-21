package io.github.crabzilla.stack;

import io.github.crabzilla.model.DomainEvent;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import java.util.List;

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
      val stream = uowList.stream()
              .flatMap(uowdata -> uowdata.getEvents().stream()
                      .map(e -> new EventProjectorTuple(uowdata.getTargetId(), e)));
      stream.forEach(tuple2 -> write(dao, tuple2.getId(), tuple2.getEvent()));
      handle.commit();
    } catch (Exception e) {
      log.error("Error with eventChannel " + eventsChannelId, e);
      handle.rollback();
    }
    log.info("Wrote {} units for eventChannel {}", uowList.size(), eventsChannelId);
  }

  public abstract void write(DAO dao, String targetId, DomainEvent event);

  @Value
  private static class EventProjectorTuple {
    public final String id;
    public final DomainEvent event;
  }

}
