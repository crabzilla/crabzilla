package io.github.crabzilla.example1

import org.jdbi.v3.sqlobject.statement.SqlQuery

// tag::dao[]
interface CustomerSummaryDao {

  @SqlQuery("select id, name, is_active from customer_summary")
  fun getAll(): List<CustomerSummary>

}
// end::dao[]
