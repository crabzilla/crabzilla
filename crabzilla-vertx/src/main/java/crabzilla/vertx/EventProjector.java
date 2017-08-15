package crabzilla.vertx;

import crabzilla.model.DomainEvent;
import javaslang.Tuple;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jdbi.v3.core.Jdbi;

import java.util.List;

@Slf4j
@AllArgsConstructor
public abstract class EventProjector<DAO> {

  protected String eventsChannelId;
  protected Class<DAO> daoClass;
  Jdbi jdbi;

  public void handle(final List<ProjectionData> uowList) {

    log.info("Writing {} units for eventChannel {}", uowList.size(), eventsChannelId);

    val handle = jdbi.open();
    val dao = handle.attach(daoClass);

    try {

      val streamOfTuple2 = uowList.stream()
              .flatMap(uowdata -> uowdata.getEvents().stream()
                      .map(e -> Tuple.of(uowdata.getTargetId(), e)));

      streamOfTuple2.forEach(tuple2 -> write(dao, tuple2._1(), tuple2._2()));

      handle.commit();

    } catch (Exception e) {

      log.error("Error with eventChannel " + eventsChannelId, e);

      handle.rollback();

    }

    log.info("Wrote {} units for eventChannel {}", uowList.size(), eventsChannelId);

  }

  public abstract void write(DAO dao, String targetId, DomainEvent event);

}
