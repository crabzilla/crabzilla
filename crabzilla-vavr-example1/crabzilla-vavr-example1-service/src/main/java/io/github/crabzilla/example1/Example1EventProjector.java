package io.github.crabzilla.example1;

import io.vavr.API;
import lombok.extern.slf4j.Slf4j;
import io.github.crabzilla.example1.customer.CustomerData;
import io.github.crabzilla.example1.readmodel.CustomerSummary;
import io.github.crabzilla.model.DomainEvent;
import io.github.crabzilla.stack.EventProjector;
import org.jdbi.v3.core.Jdbi;

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
      API.Case(API.$(instanceOf(CustomerData.CustomerCreated.class)), e ->
        run(() -> dao.insert(
                new CustomerSummary(e.getId().stringValue(), e.getName(), false)))),
      API.Case(API.$(instanceOf(CustomerData.CustomerActivated.class)), e ->
        run(() -> dao.updateStatus(targetId, true))),
      API.Case(API.$(instanceOf(CustomerData.CustomerDeactivated.class)), e ->
        run(() -> dao.updateStatus(targetId, false))),
      Case($(), o -> run(() -> {
        log.warn("{} does not have any event projection handler", event);
      })));

  }

}
