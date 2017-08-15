package example1.dao;

import crabzilla.example1.readmodel.CustomerSummary;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

public interface CustomerSummaryDao {

  @SqlUpdate("insert into customer_summary (id, name, is_active) values " +
          "(:customerSummary.id, :customerSummary.name, false)")
  void insert(CustomerSummary customerSummary);

  @SqlUpdate("update customer_summary set customer_summary.is_active = :isActive " +
          "where customer_summary.id = :id")
  void updateStatus(String id, Boolean isActive);

  @SqlQuery("select id, name, is_active from customer_summary")
  List<CustomerSummary> getAll() ;

}
