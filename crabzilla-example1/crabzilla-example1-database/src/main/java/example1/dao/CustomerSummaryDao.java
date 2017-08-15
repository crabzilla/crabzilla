package example1.dao;

import crabzilla.example1.readmodel.CustomerSummary;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

public interface CustomerSummaryDao {

  @SqlUpdate("insert into customer_summary (id, name, is_active) values " +
          "(:id, :name, false)")
  void insert(@BindBean CustomerSummary customerSummary);

  @SqlUpdate("update customer_summary set customer_summary.is_active = ? " +
          "where customer_summary.id = ?")
  void updateStatus(String id, Boolean isActive);

  @SqlQuery("select id, name, is_active from customer_summary")
  List<CustomerSummary> getAll() ;

}
