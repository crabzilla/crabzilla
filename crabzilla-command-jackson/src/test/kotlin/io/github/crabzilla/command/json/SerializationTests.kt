package io.github.crabzilla.command.json

import com.fasterxml.jackson.module.kotlin.readValue
import io.github.crabzilla.Jackson.objectMapper
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerActivated
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRegistered
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SerializationTests {

  @Test
  @DisplayName("Command ser/der")
  fun testCmd() {
    val command = RegisterCustomer(customerId = UUID.randomUUID(), name = "name1")
    val expectedJson = """
      {"type":"RegisterCustomer","customerId":"${command.customerId}","name":"${command.name}"}
    """.trimIndent()
    assertThat(objectMapper.writeValueAsString(command)).isEqualTo(expectedJson)
    assertThat(objectMapper.readValue<CustomerCommand>(expectedJson)).isEqualTo(command)
  }

  @Test
  @DisplayName("Event ser/der")
  fun testEvent() {
    val event = CustomerRegistered(UUID.randomUUID(), name = "name1")
    val expectedJson = """
      {"type":"CustomerRegistered","id":"${event.id}","name":"${event.name}"}
    """.trimIndent()
    assertThat(objectMapper.writeValueAsString(event)).isEqualTo(expectedJson)
    assertThat(objectMapper.readValue<CustomerEvent>(expectedJson)).isEqualTo(event)
  }

  @Test
  @DisplayName("DateTime ser/der")
  fun testLd() {
    val t = BeanLocalDate(LocalDate.now())
    val expectedJson = """{"ld":"${t.ld}"}"""
    assertThat(objectMapper.readValue<BeanLocalDate>(expectedJson)).isEqualTo(t)
    val resultJson = objectMapper.writeValueAsString(t)
    assertThat(objectMapper.readValue<BeanLocalDate>(resultJson)).isEqualTo(t)
  }

  @Test
  @DisplayName("LocalDateTime ser/der")
//  @RepeatedTest(100)
  fun testLdt() {
    val t = BeanLocalDateTime(LocalDateTime.now(), "foo")
    val expectedJson = """{"ldt":"${t.ldt}","newProp1":"foo"}"""
    assertThat(objectMapper.readValue<BeanLocalDateTime>(expectedJson)).isEqualTo(t)
    val resultJson = objectMapper.writeValueAsString(t)
    assertThat(objectMapper.readValue<BeanLocalDateTime>(resultJson)).isEqualTo(t)
  }

  @Test
  @DisplayName("Value Object ser/der")
  fun testVo() {
    val t = BeanValueObject(AValueObject("test", 22), "foo")
    val expectedJson = """{"vo":{"x":"test","y":22},"newProp1":"foo"}"""
    assertThat(objectMapper.writeValueAsString(t)).isEqualTo(expectedJson)
    assertThat(objectMapper.readValue<BeanValueObject>(expectedJson)).isEqualTo(t)
  }

  @Test
  fun cl() {
    val c1 = RegisterCustomer(customerId = UUID.randomUUID(), name = "name1")
    val c2 = ActivateCustomer("ya")
    data class CustomerCommands(val events: List<CustomerCommand>) // a container is needed
    val obj = CustomerCommands(listOf(c1, c2))
    val asJson = objectMapper.writeValueAsString(obj)
    assertThat(objectMapper.readValue<CustomerCommands>(asJson)).isEqualTo(obj)
  }

  @Test
  fun el() {
    val event1 = CustomerRegistered(UUID.randomUUID(), name = "name1")
    val event2 = CustomerActivated("because yes")
    data class CustomerEvents(val events: List<CustomerEvent>) // a container is needed
    val obj = CustomerEvents(listOf(event1, event2))
    val asJson = objectMapper.writeValueAsString(obj)
    assertThat(objectMapper.readValue<CustomerEvents>(asJson)).isEqualTo(obj)
  }

  @Test
  @DisplayName("BigDecimal")
  fun testDecimal() {
    val amount = BeanBigDecimal(BigDecimal("34.56332"))
    val asJson = objectMapper.writeValueAsString(amount)
    assertThat(objectMapper.readValue<BeanBigDecimal>(asJson)).isEqualTo(amount)
  }
}

data class AValueObject(val x: String, val y: Int)

data class BeanValueObject(val vo: AValueObject, val newProp1: String? = null)

data class BeanLocalDateTime(val ldt: LocalDateTime, val newProp1: String? = null)

data class BeanLocalDate(val ld: LocalDate, val newProp1: String? = null)

data class BeanBigDecimal(val amount: BigDecimal)
