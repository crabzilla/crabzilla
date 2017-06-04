package crabzilla.example1.projectors;

import crabzilla.example1.aggregates.customer.events.CustomerActivated;
import crabzilla.example1.aggregates.customer.events.CustomerCreated;
import crabzilla.example1.aggregates.customer.events.CustomerDeactivated;
import crabzilla.model.Event;
import crabzilla.model.EventsProjector;
import crabzilla.model.ProjectionData;
import javaslang.Tuple;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Configuration;
import org.jooq.impl.DSL;

import java.util.List;

import static example1.datamodel.tables.CustomerSummary.CUSTOMER_SUMMARY;
import static javaslang.API.*;
import static javaslang.Predicates.instanceOf;

@Slf4j
public class Example1EventsProjector implements EventsProjector {

  @Getter
  private final String eventsChannelId;
  private final Configuration jooqCfg;

  public Example1EventsProjector(@NonNull final String eventsChannelId, @NonNull final Configuration jooqCfg) {
    this.eventsChannelId = eventsChannelId;
    this.jooqCfg = jooqCfg;
  }

  @Override
  public Long getLastUowSeq() {
    return null; // TODO
  }

  @Override
  public void handle(final List<ProjectionData> uowList) {

    log.info("writing {} events for eventsChannelId {}", uowList.size(), eventsChannelId);

    DSL.using(jooqCfg)
      .transaction(ctx -> uowList.stream()
              .flatMap(uowdata -> uowdata.getEvents().stream()
              .map(e -> Tuple.of(uowdata.getTargetId(), e)))
              .forEach(tuple -> handle(ctx, tuple._1(), tuple._2())));

    log.info("wrote {} events for eventsChannelId {}", uowList.size(), eventsChannelId);
  }


  public void handle(final Configuration ctx, final String id, final Event event) {

    log.info("event {} from channel {}", event, eventsChannelId);

    Match(event).of(

      Case(instanceOf(CustomerCreated.class), (e) ->
              run(() -> DSL.using(ctx).insertInto(CUSTOMER_SUMMARY)
                      .values(e.getId(), e.getName(), false))
      ),

      Case(instanceOf(CustomerActivated.class), (e) ->
              run(() -> DSL.using(ctx).update(CUSTOMER_SUMMARY)
                                      .set(CUSTOMER_SUMMARY.IS_ACTIVE, true)
                                      .where(CUSTOMER_SUMMARY.ID.eq(id)))

      ),

      Case(instanceOf(CustomerDeactivated.class), (e) ->
              run(() -> DSL.using(ctx).update(CUSTOMER_SUMMARY)
                      .set(CUSTOMER_SUMMARY.IS_ACTIVE, false)
                      .where(CUSTOMER_SUMMARY.ID.eq(id)))
      ),

      Case($(), e -> run(() -> log.warn("{} does not have any event projection handler", e)))

    );

    // TODO update uow_last_seq for this event channel

  }

}
