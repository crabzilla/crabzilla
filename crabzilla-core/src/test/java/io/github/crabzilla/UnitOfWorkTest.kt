package io.github.crabzilla

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.UnitOfWork
import io.github.crabzilla.example1.CreateCustomer
import io.github.crabzilla.example1.CustomerCreated
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class UnitOfWorkTest {

  @Test
  fun versionZeroCannotBeInstantiated() {
    assertThrows(RuntimeException::class.java, {
      UnitOfWork("customer", 1, UUID.randomUUID(), CreateCustomer("cust#1"), 0,
              listOf<DomainEvent>(CustomerCreated(1, "cust#1")))
    }, "version should be >= 1")
  }

  @Test
  fun versionOneCanBeInstantiated() {
    val command = CreateCustomer("cust#1")
    UnitOfWork("customer", 1, UUID.randomUUID(), command, 1, listOf<DomainEvent>(CustomerCreated(1, "cust#1")))
  }

  @Test
  fun versionLessThanZeroCannotBeInstantiated() {
    assertThrows(RuntimeException::class.java, {
      UnitOfWork("customer", 1, UUID.randomUUID(), CreateCustomer("cust#1"), -1,
              listOf<DomainEvent>(CustomerCreated(1, "cust#1")))
    }, "version should be >= 1")
  }
}
