package io.github.crabzilla.core

import io.github.crabzilla.example1.customer.CreateCustomer
import io.github.crabzilla.example1.customer.CustomerCreated
import io.github.crabzilla.example1.customer.CustomerId
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*
import java.util.Arrays.asList

@DisplayName("An CommandResult")
class CommandResultTest {

  internal lateinit var result: CommandResult

  internal val version = 0L
  internal val customerId = CustomerId("c1")
  internal val commandId = UUID.randomUUID()
  internal val event: DomainEvent = CustomerCreated(customerId, "c1")
  internal val uow = UnitOfWork(UUID.randomUUID(),
    CreateCustomer(commandId, customerId, "c1"),
    version,
    asList(event))

  @Test
  @DisplayName("Success can be instantiated")
  fun successCanBeInstantiated() {
    result = CommandResult.success(uow)
    assertThat(result).isNotNull()
  }

  @Test
  @DisplayName("Success can not be null")
  fun successCannotBeNull() {
    assertThrows(NullPointerException::class.java) { result = CommandResult.success(null!!) }
  }

  @Test
  @DisplayName("Error can be instantiated")
  fun errorCanBeInstantiated() {
    result = CommandResult.error(RuntimeException("test"))
    assertThat(result).isNotNull()
  }

  @Test
  @DisplayName("Error cannot be null")
  fun errorCannnotBeNull() {
    assertThrows(NullPointerException::class.java) { result = CommandResult.error(null!!) }
  }

  @Nested
  @DisplayName("When is success")
  inner class WhenIsSuccess {

    @BeforeEach
    internal fun setUp() {
      result = CommandResult.success(uow)
    }

    @Test
    @DisplayName("success must run success block")
    internal fun successMustRunSuccessBlock() {
      result.inCaseOfSuccess { uow -> assertThat(result).isNotNull() }
    }

    @Test
    @DisplayName("success cannot run an error block")
    internal fun successMustNotRunErrorBlock() {
      result.inCaseOfError { uow -> fail<Unit>("success cannot run an error block") }
    }

  }

  @Nested
  @DisplayName("When is error")
  inner class WhenIsError {

    @BeforeEach
    internal fun setUp() {
      result = CommandResult.error(RuntimeException("test"))
    }

    @Test
    @DisplayName("error must run error block")
    internal fun errorMustRunErrorBlock() {
      result.inCaseOfError { uow -> assertThat(result).isNotNull() }
    }

    @Test
    @DisplayName("error cannot run an success block")
    internal fun errorMustNotRunSuccessBlock() {
      result.inCaseOfSuccess { uow -> fail<Unit>("error cannot run an success block") }
    }

  }
}
