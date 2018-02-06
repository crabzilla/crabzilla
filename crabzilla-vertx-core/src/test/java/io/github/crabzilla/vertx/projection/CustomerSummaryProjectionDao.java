package io.github.crabzilla.vertx.projection;

import io.github.crabzilla.example1.CustomerSummary;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface CustomerSummaryProjectionDao {

  @SqlUpdate("insert into customer_summary (id, name, is_active) values " +
          "(:id, :name, false)")
  void insert(@BindBean CustomerSummary customerSummary);

//  @SqlUpdate("update customer_summary set customer_summary.is_active = :isActive " +
//          "where customer_summary.id = :id")
//  void updateStatus(@Bind("id") String id, @Bind("isActive") Boolean isActive);
//
//  @SqlQuery("select id, name, is_active from customer_summary")
//  @RegisterBeanMapper(CustomerSummary.class)
//  List<CustomerSummary> getAll();

}
