package io.github.crabzilla

import io.github.crabzilla.example1.CreateCustomer
import io.github.crabzilla.example1.CustomerCommandEnum
import io.github.crabzilla.example1.CustomerCreated
import io.github.crabzilla.example1.CustomerId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

class ProjectionDataTest {

  @Test
  fun fromUow() {

    val command = CreateCustomer("cust#1")

    val uow = UnitOfWork(UUID.randomUUID(), "customer", 1, UUID.randomUUID(), CustomerCommandEnum.CREATE.urlFriendly(),
      command, 1, listOf<Pair<String, DomainEvent>>(Pair("CustomerCreated", CustomerCreated(CustomerId(1), "cust#1"))))

    val pd = ProjectionData.fromUnitOfWork(1, uow)

    assertThat(pd.uowSequence).isEqualTo(1)
    assertThat(pd.uowId).isEqualTo(uow.unitOfWorkId)
    assertThat(pd.entityId).isEqualTo(uow.entityId)
    assertThat(pd.events).isEqualTo(uow.events)

  }

}
