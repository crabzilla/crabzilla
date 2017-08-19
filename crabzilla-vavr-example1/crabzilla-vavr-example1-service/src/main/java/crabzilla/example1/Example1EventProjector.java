package crabzilla.example1;

import crabzilla.example1.customer.CustomerData.CustomerCreated;
import crabzilla.example1.readmodel.CustomerSummary;
import crabzilla.model.DomainEvent;
import crabzilla.stack.EventProjector;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;

import static crabzilla.example1.customer.CustomerData.CustomerActivated;
import static crabzilla.example1.customer.CustomerData.CustomerDeactivated;
import static io.vavr.API.*;
import static io.vavr.Predicates.instanceOf;

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
      Case($(), o -> run(() -> {
        log.warn("{} does not have any event projection handler", event);
      })));

  }

}
