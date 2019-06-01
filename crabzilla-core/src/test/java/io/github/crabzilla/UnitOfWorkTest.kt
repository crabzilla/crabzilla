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
      UnitOfWork("customer", 1, UUID.randomUUID(), "create",
        CreateCustomer("cust#1"), 0,
        listOf<Pair<String, DomainEvent>>(Pair("CustomerCreated", CustomerCreated(CustomerId(1), "cust#1"))))
    }, "version should be >= 1")
  }

  @Test
  fun versionOneCanBeInstantiated() {
    val command = CreateCustomer("cust#1")
    UnitOfWork("customer", 1, UUID.randomUUID(), "create",
      command, 1,
      listOf<Pair<String, DomainEvent>>(Pair("CustomerCreated", CustomerCreated(CustomerId(1), "cust#1"))))
  }

  @Test
  fun versionLessThanZeroCannotBeInstantiated() {
    assertThrows(RuntimeException::class.java, {
      UnitOfWork("customer", 1, UUID.randomUUID(), "create",
        CreateCustomer("cust#1"), -1,
        listOf<Pair<String, DomainEvent>>(Pair("CustomerCreated", CustomerCreated(CustomerId(1), "cust#1"))))
    }, "version should be >= 1")
  }

  @Test
  fun targetIdIsEqualsToCommandTargetId() {
    val command = CreateCustomer("cust#1")
    val uow = UnitOfWork("customer", 1, UUID.randomUUID(), "create",
      command, 1,
      listOf<Pair<String, DomainEvent>>(Pair("CustomerCreated", CustomerCreated(CustomerId(1), "cust#1"))))
    assertThat(uow.entityId).isEqualTo(uow.entityId)
  }

}
