package io.github.crabzilla.internal

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.UnitOfWork
import io.github.crabzilla.example1.customer.CreateCustomer
import io.github.crabzilla.example1.customer.CustomerCreated
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UnitOfWorkEventsTest {

  @Test
  fun fromUow() {

    val command = CreateCustomer("cust#1")

    val uow =
      UnitOfWork("customer", 1, UUID.randomUUID(), command, 1, listOf<DomainEvent>(CustomerCreated(1, "cust#1")))

    val pd = UnitOfWorkEvents(1, uow.entityId, uow.events)

    assertThat(pd.uowId).isEqualTo(1)
    assertThat(pd.entityId).isEqualTo(uow.entityId)
    assertThat(pd.events).isEqualTo(uow.events)
  }
}
