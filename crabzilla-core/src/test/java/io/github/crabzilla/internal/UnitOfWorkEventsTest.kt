package io.github.crabzilla.internal

import io.github.crabzilla.example1.CreateCustomer
import io.github.crabzilla.example1.CustomerCreated
import io.github.crabzilla.example1.CustomerId
import io.github.crabzilla.framework.DomainEvent
import io.github.crabzilla.framework.UnitOfWork
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

class UnitOfWorkEventsTest {

  @Test
  fun fromUow() {

    val command = CreateCustomer("cust#1")

    val uow = UnitOfWork("customer", 1, UUID.randomUUID(), "create",
      command, 1, listOf<Pair<String, DomainEvent>>(Pair("CustomerCreated", CustomerCreated(CustomerId(1), "cust#1"))))

    val pd = fromUnitOfWork(1, uow)

    assertThat(pd.uowId).isEqualTo(1)
    assertThat(pd.entityId).isEqualTo(uow.entityId)
    assertThat(pd.events).isEqualTo(uow.events)

  }

}
