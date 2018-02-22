package io.github.crabzilla.core

import io.github.crabzilla.example1.customer.CreateCustomer
import io.github.crabzilla.example1.customer.CustomerCreated
import io.github.crabzilla.example1.customer.CustomerId
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*

@DisplayName("An CommandResult")
class CommandFunctionsTest {

  private val customerId = CustomerId("c1")
  private val commandId = UUID.randomUUID()
  private val command = CreateCustomer(commandId, customerId, "c1")
  private val event: DomainEvent = CustomerCreated(customerId, "c1")
  private val uow = UnitOfWork(UUID.randomUUID(), command, 1L, eventsOf(event))

  @Test
  @DisplayName("Success can be instantiated")
  fun resultOfSuccess() {
    val result: CommandResult = resultOf({  uow })
    assertThat(uow).isEqualTo(result.unitOfWork)
    assertThat(result.exception).isNull()
  }

  @Test
  @DisplayName("Error can be instantiated")
  fun resultOfError() {
    val exception = RuntimeException("an exception")
    val result: CommandResult = resultOf({  throw exception })
    assertThat(exception).isEqualTo(result.exception)
    assertThat(result.unitOfWork).isNull()
  }

  @Test
  @DisplayName("uowOf")
  fun uowOf() {
    assertThat(uow).isEqualToIgnoringGivenFields(uowOf(command, eventsOf(event), 0), "unitOfWorkId")
  }

}
