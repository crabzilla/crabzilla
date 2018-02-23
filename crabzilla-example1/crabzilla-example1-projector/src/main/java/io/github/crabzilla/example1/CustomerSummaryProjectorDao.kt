package io.github.crabzilla.example1

import org.jdbi.v3.sqlobject.statement.SqlUpdate

// tag::dao[]
interface CustomerSummaryProjectorDao {

  @SqlUpdate("insert into customer_summary (id, name, is_active) values " +
          "(:customerSummary.id, :customerSummary.name, false)")
  fun insert(customerSummary: CustomerSummary)

  @SqlUpdate("update customer_summary set customer_summary.is_active = :isActive " +
          "where customer_summary.id = :id")
  fun updateStatus(id: String, isActive: Boolean)

}
// end::dao[]