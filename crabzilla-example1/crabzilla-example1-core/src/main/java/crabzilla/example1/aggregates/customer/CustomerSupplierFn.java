package crabzilla.example1.aggregates.customer;

import java.util.function.Supplier;

public class CustomerSupplierFn implements Supplier<Customer> {
  final Customer customer = new Customer(null, null,  null, false, null);
  @Override
  public Customer get() {
    return customer;
  }
}
