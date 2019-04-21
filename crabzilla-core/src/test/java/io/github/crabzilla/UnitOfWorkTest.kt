package io.github.crabzilla

import io.github.crabzilla.example1.CreateCustomer
import io.github.crabzilla.example1.CustomerCreated
import io.github.crabzilla.example1.CustomerId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.*

class UnitOfWorkTest {

  @Test
  fun versionZeroCannotBeInstantiated() {
    assertThrows(RuntimeException::class.java, {
      UnitOfWork(UUID.randomUUID(), "Customer", 1, UUID.randomUUID(), "CreateCustomer",
        CreateCustomer(CustomerId(1), "cust#1"), 0, listOf<DomainEvent>(CustomerCreated(CustomerId(1), "cust#1")))
    }, "version should be >= 1")
  }

  @Test
  fun versionOneCanBeInstantiated() {
    val command = CreateCustomer(CustomerId(1), "cust#1")
    UnitOfWork(UUID.randomUUID(), "Customer", 1, UUID.randomUUID(), "CreateCustomer", command, 1,
      listOf<DomainEvent>(CustomerCreated(CustomerId(1), "cust#1")))
  }

  @Test
  fun versionLessThanZeroCannotBeInstantiated() {
    assertThrows(RuntimeException::class.java, {
      UnitOfWork(UUID.randomUUID(), "Customer", 1, UUID.randomUUID(), "CreateCustomer",
        CreateCustomer(CustomerId(1), "cust#1"), -1,
        listOf<DomainEvent>(CustomerCreated(CustomerId(1), "cust#1")))
    }, "version should be >= 1")
  }

  @Test
  fun targetIdIsEqualsToCommandTargetId() {
    val command = CreateCustomer(CustomerId(1), "cust#1")
    val uow = UnitOfWork(UUID.randomUUID(), "Customer", 1, UUID.randomUUID(), "CreateCustomer", command, 1,
      listOf<DomainEvent>(CustomerCreated(CustomerId(1), "cust#1")))
    assertThat(uow.targetId).isEqualTo(uow.targetId)
  }

}
