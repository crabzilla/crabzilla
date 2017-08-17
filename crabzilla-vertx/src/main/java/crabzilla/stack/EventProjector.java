package crabzilla.stack;

import crabzilla.model.DomainEvent;
import lombok.val;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import java.util.List;

//@Slf4j
public abstract class EventProjector<D> {

  protected final String eventsChannelId;
  protected final Class<D> daoClass;
  protected final Jdbi jdbi;

  protected EventProjector(String eventsChannelId, Class<D> daoClass, Jdbi jdbi) {
    this.eventsChannelId = eventsChannelId;
    this.daoClass = daoClass;
    this.jdbi = jdbi;
  }

  public void handle(final List<ProjectionData> uowList) {

//    log.info("Writing {} units for eventChannel {}", uowList.size(), eventsChannelId);

    final Handle handle = jdbi.open();
    final D dao = handle.attach(daoClass);

    try {

      val stream = uowList.stream()
              .flatMap(uowdata -> uowdata.getEvents().stream()
                      .map(e -> new EventProjectorTuple(uowdata.getTargetId(), e)));

      stream.forEach(tuple2 -> write(dao, tuple2.id, tuple2.event));

      handle.commit();

    } catch (Exception e) {

//      log.error("Error with eventChannel " + eventsChannelId, e);

      handle.rollback();

    }

//    log.info("Wrote {} units for eventChannel {}", uowList.size(), eventsChannelId);

  }

  public abstract void write(D dao, String targetId, DomainEvent event);

}
