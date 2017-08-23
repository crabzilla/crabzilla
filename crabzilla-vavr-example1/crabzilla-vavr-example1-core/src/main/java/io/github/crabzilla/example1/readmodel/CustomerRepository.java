package io.github.crabzilla.example1.readmodel;

import java.util.List;

//tag::readmodel[]
public interface CustomerRepository {

  List<CustomerSummary> getAll();
}

//end::readmodel[]