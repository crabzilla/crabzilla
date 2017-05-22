package crabzilla.example1.aggregates.customer;

import java.util.function.Supplier;

public class CustomerSupplierFn implements Supplier<Customer> {
  @Override
  public Customer get() {
    return new Customer(null, null,  null, false, null);
  }
}
