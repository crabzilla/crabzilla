package crabzilla.vertx;

import crabzilla.model.DomainEvent;
import crabzilla.stack.ProjectionData;
import lombok.val;
import org.jdbi.v3.core.Jdbi;

import java.util.List;

//@Slf4j
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

//    log.info("Writing {} units for eventChannel {}", uowList.size(), eventsChannelId);

    val handle = jdbi.open();
    val dao = handle.attach(daoClass);

    try {

      val streamOfTuple2 = uowList.stream()
              .flatMap(uowdata -> uowdata.getEvents().stream()
                      .map(e -> new Tuple2(uowdata.getTargetId(), e)));

      streamOfTuple2.forEach(tuple2 -> write(dao, tuple2.id, tuple2.event));

      handle.commit();

    } catch (Exception e) {

//      log.error("Error with eventChannel " + eventsChannelId, e);

      handle.rollback();

    }

//    log.info("Wrote {} units for eventChannel {}", uowList.size(), eventsChannelId);

  }

  public abstract void write(DAO dao, String targetId, DomainEvent event);

  private class Tuple2 {
    final String id;
    final DomainEvent event;
    private Tuple2(String id, DomainEvent event) {
      this.id = id;
      this.event = event;
    }
  }

}
