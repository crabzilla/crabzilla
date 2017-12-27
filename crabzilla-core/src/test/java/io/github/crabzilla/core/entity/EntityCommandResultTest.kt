package io.github.crabzilla.core.entity

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.example1.customer.CreateCustomer
import io.github.crabzilla.example1.customer.CustomerCreated
import io.github.crabzilla.example1.customer.CustomerId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import java.util.UUID

import java.util.Arrays.asList
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.fail

@DisplayName("An EntityCommandResult")
class EntityCommandResultTest {

  internal lateinit var result: EntityCommandResult

  internal val version = Version(1)
  internal val customerId = CustomerId("c1")
  internal val commandId = UUID.randomUUID()
  internal val event: DomainEvent = CustomerCreated(customerId, "c1")
  internal val uow = EntityUnitOfWork(UUID.randomUUID(),
          CreateCustomer(commandId, customerId, "c1"),
          version,
          asList(event))

  @Test
  @DisplayName("Success can be instantiated")
  fun successCanBeInstantiated() {
    result = EntityCommandResult.success(uow)
    assertThat(result).isNotNull()
  }

  @Test
  @DisplayName("Success can not be null")
  fun successCannotBeNull() {
    assertThrows(NullPointerException::class.java) { result = EntityCommandResult.success(null!!) }
  }

  @Test
  @DisplayName("Error can be instantiated")
  fun errorCanBeInstantiated() {
    result = EntityCommandResult.error(RuntimeException("test"))
    assertThat(result).isNotNull()
  }

  @Test
  @DisplayName("Error cannot be null")
  fun errorCannnotBeNull() {
    assertThrows(NullPointerException::class.java) { result = EntityCommandResult.error(null!!) }
  }

  @Nested
  @DisplayName("When is success")
  inner class WhenIsSuccess {

    @BeforeEach
    internal fun setUp() {
      result = EntityCommandResult.success(uow)
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
      result = EntityCommandResult.error(RuntimeException("test"))
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