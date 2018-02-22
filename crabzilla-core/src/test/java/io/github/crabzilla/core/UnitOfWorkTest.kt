package io.github.crabzilla.core

import io.github.crabzilla.example1.customer.CreateCustomer
import io.github.crabzilla.example1.customer.CustomerCreated
import io.github.crabzilla.example1.customer.CustomerId
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.*

class UnitOfWorkTest {

  @Test
  fun versionZeroCannotBeInstantiated() {
    assertThrows(RuntimeException::class.java, {
      UnitOfWork(UUID.randomUUID(), CreateCustomer(UUID.randomUUID(), CustomerId("1"), "cust#1"), 0,
        listOf<DomainEvent>(CustomerCreated(CustomerId("1"), "cust#1")))
    }, "version should be >= 1")
  }

  @Test
  fun versionOneCanBeInstantiated() {
    UnitOfWork(UUID.randomUUID(), CreateCustomer(UUID.randomUUID(), CustomerId("1"), "cust#1"), 1,
      listOf<DomainEvent>(CustomerCreated(CustomerId("1"), "cust#1")))
  }

  @Test
  fun versionLessThanZeroCannotBeInstantiated() {
    assertThrows(RuntimeException::class.java, {
      UnitOfWork(UUID.randomUUID(), CreateCustomer(UUID.randomUUID(), CustomerId("1"), "cust#1"), -1,
        listOf<DomainEvent>(CustomerCreated(CustomerId("1"), "cust#1")))
    }, "version should be >= 1")
  }


}
