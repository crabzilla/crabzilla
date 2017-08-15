package crabzilla.example1;

import crabzilla.model.DomainEvent;
import crabzilla.vertx.EventProjector;
import example1.dao.CustomerSummaryDao;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;

@Slf4j
public class Example1EventProjector extends EventProjector<CustomerSummaryDao> {

  Example1EventProjector(String eventsChannelId, Class<CustomerSummaryDao> daoClass, Jdbi jdbi) {
    super(eventsChannelId, daoClass, jdbi);
  }

  @Override
  public void write(CustomerSummaryDao customerSummaryDao, String targetId, DomainEvent event) {

    log.info("event {} from channel {}", event, eventsChannelId);

  }

}
