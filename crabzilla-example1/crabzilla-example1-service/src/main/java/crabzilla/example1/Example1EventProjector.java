package crabzilla.example1;

import crabzilla.example1.readmodel.CustomerSummary;
import crabzilla.example1.util.AbstractExample1EventProjector;
import example1.dao.CustomerSummaryDao;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Jdbi;

import static crabzilla.example1.customer.CustomerData.*;

@Slf4j
public class Example1EventProjector extends AbstractExample1EventProjector<CustomerSummaryDao> {

  protected Example1EventProjector(String eventsChannelId,
                                   Class<CustomerSummaryDao> customerSummaryDaoClass, Jdbi jdbi) {
    super(eventsChannelId, customerSummaryDaoClass, jdbi);
  }

  public void handle(CustomerSummaryDao dao, String targetId, CustomerCreated event) {
    dao.insert(new CustomerSummary(targetId, event.getName(), false));
  }

  public void handle(CustomerSummaryDao dao, String targetId, CustomerActivated event) {
    dao.updateStatus(targetId, true);
  }

  public void handle(CustomerSummaryDao dao, String targetId, CustomerDeactivated event) {
    dao.updateStatus(targetId, false);
  }
}
