package io.github.crabzilla.vertx.projection;

import io.github.crabzilla.core.DomainEvent;
import io.github.crabzilla.vertx.ProjectionData;
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

  /**
   * TODO try to use 1 transaction for all events (db with auto-commit = false)
   * @param uowList
   */
  public void handle(final List<ProjectionData> uowList) {
    log.info("Writing {} units for eventChannel {}", uowList.size(), eventsChannelId);
    final DAO dao = jdbi.onDemand(daoClass);
    final Stream<TargetIDDomainEventPair> stream = uowList.stream()
            .flatMap(uowData -> uowData.getEvents().stream()
                    .map(e -> new TargetIDDomainEventPair(uowData.getTargetId(), e)));
    stream.forEach(tuple2 -> write(dao, tuple2.getId(), tuple2.getEvent()));//

//    try (Handle handle = jdbi.open()) {
//      handle.inTransaction(h -> {
//        handle.begin();
//        final DAO dao = handle.attach(daoClass);
//        final Stream<TargetIDDomainEventPair> stream = uowList.stream()
//            .flatMap(uowData -> uowData.getEvents().stream()
//                    .map(e -> new TargetIDDomainEventPair(uowData.getTargetId(), e)));
//        stream.forEach(tuple2 -> write(dao, tuple2.getId(), tuple2.getEvent()));
//        handle.commit();
//        log.info("Wrote {} units for eventChannel {}", uowList.size(), eventsChannelId);
//        return 1;
//      });
//    }

  }

  public abstract void write(DAO dao, String targetId, DomainEvent event);

}
