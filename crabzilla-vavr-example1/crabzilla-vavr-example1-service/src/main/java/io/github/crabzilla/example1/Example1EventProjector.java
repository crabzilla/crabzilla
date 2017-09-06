package io.github.crabzilla.example1;

import io.github.crabzilla.core.DomainEvent;
import io.github.crabzilla.example1.readmodel.CustomerSummary;
import io.github.crabzilla.vertx.projection.EventProjector;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;

import static io.github.crabzilla.example1.customer.CustomerData.*;
import static io.vavr.API.*;
import static io.vavr.Predicates.instanceOf;

// tag::projector[]
@Slf4j
public class Example1EventProjector extends EventProjector<CustomerSummaryDao> {

  Example1EventProjector(String eventsChannelId, Class<CustomerSummaryDao> daoClass, Jdbi jdbi) {
    super(eventsChannelId, daoClass, jdbi);
  }

  @Override
  public void write(CustomerSummaryDao dao, String targetId, DomainEvent event) {

    log.info("writing event {} from channel {}", event, eventsChannelId);

    Match(event).of(
      Case($(instanceOf(CustomerCreated.class)), e ->
        run(() -> dao.insert(
                new CustomerSummary(e.getId().stringValue(), e.getName(), false)))),
      Case($(instanceOf(CustomerActivated.class)), e ->
        run(() -> dao.updateStatus(targetId, true))),
      Case($(instanceOf(CustomerDeactivated.class)), e ->
        run(() -> dao.updateStatus(targetId, false))),
      Case($(), o -> run(() -> log.warn("{} does not have any event projection handler", event))));
  }

}
// end::projector[]
