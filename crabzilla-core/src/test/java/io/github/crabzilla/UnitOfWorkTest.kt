package io.github.crabzilla

import io.github.crabzilla.example1.*
import io.github.crabzilla.example1.CustomerCommandEnum.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.*

class UnitOfWorkTest {

  @Test
  fun versionZeroCannotBeInstantiated() {
    assertThrows(RuntimeException::class.java, {
      UnitOfWork(UUID.randomUUID(), "customer", 1, UUID.randomUUID(), CREATE.urlFriendly(),
        CreateCustomer(CustomerId(1), "cust#1"), 0, listOf<DomainEvent>(CustomerCreated(CustomerId(1), "cust#1")))
    }, "version should be >= 1")
  }

  @Test
  fun versionOneCanBeInstantiated() {
    val command = CreateCustomer(CustomerId(1), "cust#1")
    UnitOfWork(UUID.randomUUID(), "customer", 1, UUID.randomUUID(), CREATE.urlFriendly(),
      command, 1, listOf<DomainEvent>(CustomerCreated(CustomerId(1), "cust#1")))
  }

  @Test
  fun versionLessThanZeroCannotBeInstantiated() {
    assertThrows(RuntimeException::class.java, {
      UnitOfWork(UUID.randomUUID(), "customer", 1, UUID.randomUUID(),
        CREATE.urlFriendly(), CreateCustomer(CustomerId(1), "cust#1"), -1,
        listOf<DomainEvent>(CustomerCreated(CustomerId(1), "cust#1")))
    }, "version should be >= 1")
  }

  @Test
  fun targetIdIsEqualsToCommandTargetId() {
    val command = CreateCustomer(CustomerId(1), "cust#1")
    val uow = UnitOfWork(UUID.randomUUID(), "customer", 1, UUID.randomUUID(),
      CREATE.urlFriendly(), command, 1, listOf<DomainEvent>(CustomerCreated(CustomerId(1), "cust#1")))
    assertThat(uow.entityId).isEqualTo(uow.entityId)
  }

}
