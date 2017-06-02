package crabzilla.example1.projectors;

import crabzilla.example1.aggregates.customer.events.CustomerCreated;
import lombok.val;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

public class Example1EventsProjectorJdbi extends AbstractEventsProjectorJdbi {

  public Example1EventsProjectorJdbi(String eventsChannelId, DBI dbi) {
    super(eventsChannelId, dbi);
  }

  public void handle(final String id, final CustomerCreated event) {

    val dao = dbi.open(CustomerDao.class);
    dao.insert(event.getId().getStringValue(), event.getName());
    dao.close();

  }

  interface CustomerDao {

    @SqlUpdate("insert into customer_summary (id, name, is_active) values (:id, :name, false)")
    void insert(@Bind("id") String id, @Bind("name") String name);

    void close();

  }

}
