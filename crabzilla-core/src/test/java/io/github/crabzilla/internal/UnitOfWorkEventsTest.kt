package io.github.crabzilla.internal

import io.github.crabzilla.example1.customer.CreateCustomer
import io.github.crabzilla.example1.customer.CustomerCreated
import io.github.crabzilla.framework.DomainEvent
import io.github.crabzilla.framework.UnitOfWork
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

class UnitOfWorkEventsTest {

  @Test
  fun fromUow() {

    val command = CreateCustomer("cust#1")

    val uow =
      UnitOfWork("customer", 1, UUID.randomUUID(), command, 1, listOf<DomainEvent>(CustomerCreated(1, "cust#1")))

    val pd = fromUnitOfWork(1, uow)

    assertThat(pd.uowId).isEqualTo(1)
    assertThat(pd.entityId).isEqualTo(uow.entityId)
    assertThat(pd.events).isEqualTo(uow.events)

  }

}
