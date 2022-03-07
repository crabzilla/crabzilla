package io.github.crabzilla.core.json

import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRegistered
import io.github.crabzilla.example1.customer.customerConfig
import io.github.crabzilla.example1.customer.example1Json
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SerializationTests {

  @Test
  @DisplayName("State ser/der")
  fun testState() {
    val aggregate = Customer(UUID.randomUUID(), "name1")
    val expectedJson = """
      {"type":"Customer","id":"${aggregate.id}","name":"${aggregate.name}"}
    """.trimIndent()
    assertThat(example1Json.encodeToString(customerConfig.stateSerDer, aggregate)).isEqualTo(expectedJson)
    assertThat(example1Json.decodeFromString(customerConfig.stateSerDer, expectedJson)).isEqualTo(aggregate)
  }

  @Test
  @DisplayName("Command ser/der")
  fun testCmd() {
    val command = RegisterCustomer(customerId = UUID.randomUUID(), name = "name1")
    val expectedJson = """
      {"type":"RegisterCustomer","customerId":"${command.customerId}","name":"${command.name}"}
    """.trimIndent()
    assertThat(example1Json.encodeToString(customerConfig.commandSerDer, command)).isEqualTo(expectedJson)
    assertThat(example1Json.decodeFromString(customerConfig.commandSerDer, expectedJson)).isEqualTo(command)
  }

  @Test
  @DisplayName("Event ser/der")
  fun testEvent() {
    val event = CustomerRegistered(UUID.randomUUID(), name = "name1")
    val expectedJson = """
      {"type":"CustomerRegistered","id":"${event.id}","name":"${event.name}"}
    """.trimIndent()
    assertThat(example1Json.encodeToString(customerConfig.eventSerDer, event)).isEqualTo(expectedJson)
    assertThat(example1Json.decodeFromString(customerConfig.eventSerDer, expectedJson)).isEqualTo(event)
  }

  @Test
  @DisplayName("DateTime ser/der")
  fun testLd() {
    val t = BeanLocalDate(LocalDate.now())
    val expectedJson = """{"ld":"${t.ld}"}"""
    assertThat(example1Json.decodeFromString<BeanLocalDate>(expectedJson)).isEqualTo(t)
    val resultJson = example1Json.encodeToString(t)
    assertThat(example1Json.decodeFromString<BeanLocalDate>(resultJson)).isEqualTo(t)
  }

  @Test
  @DisplayName("LocalDateTime ser/der")
//  @RepeatedTest(100)
  fun testLdt() {
    val t = BeanLocalDateTime(LocalDateTime.now(), "foo")
    val expectedJson = """{"ldt":"${t.ldt}","newProp1":"foo"}"""
    assertThat(example1Json.decodeFromString<BeanLocalDateTime>(expectedJson)).isEqualTo(t)
    val resultJson = example1Json.encodeToString(t)
    assertThat(example1Json.decodeFromString<BeanLocalDateTime>(resultJson)).isEqualTo(t)
  }

  @Test
  @DisplayName("Value Object ser/der")
  fun testVo() {
    val t = BeanValueObject(AValueObject("test", 22), "foo")
    val expectedJson = """{"vo":{"x":"test","y":22},"newProp1":"foo"}"""
    assertThat(example1Json.encodeToString(t)).isEqualTo(expectedJson)
    assertThat(example1Json.decodeFromString<BeanValueObject>(expectedJson)).isEqualTo(t)
  }

  @Test
  @DisplayName("Command List ser/der")
  fun testDomainList() {
    val c1 = RegisterCustomer(customerId = UUID.randomUUID(), name = "name1")
    val c2 = ActivateCustomer("ya")
    val list = listOf(c1, c2)
    val expectedJson = """[{"type":"RegisterCustomer","customerId":"${c1.customerId}",
      "name":"${c1.name}"},{"type":"ActivateCustomer","reason":"${c2.reason}"}]"""
    assertThat(example1Json.encodeToString(list)).isEqualToIgnoringWhitespace(expectedJson)
    assertThat(example1Json.decodeFromString<List<CustomerCommand>>(expectedJson)).isEqualTo(list)
  }

  @Test
  @DisplayName("Event List ser/der")
  fun testEventList() {
    val event1 = CustomerRegistered(UUID.randomUUID(), name = "name1")
    val event2 = CustomerEvent.CustomerActivated("because yes")
    val asJson = example1Json.encodeToString(listOf(event1, event2))
    assertThat(example1Json.decodeFromString<List<CustomerEvent>>(asJson)).isEqualTo(listOf(event1, event2))
  }

  @Test
  @DisplayName("BigDecimal")
  fun testDecimal() {
    val amount = BeanBigDecimal(BigDecimal("34.56332"))
    val asJson = example1Json.encodeToString(amount)
    assertThat(example1Json.decodeFromString<BeanBigDecimal>(asJson)).isEqualTo(amount)
  }


}

@Serializable
data class AValueObject(val x: String, val y: Int)

@Serializable
data class BeanValueObject(@Contextual val vo: AValueObject, val newProp1: String? = null)

@Serializable
data class BeanLocalDateTime(@Contextual val ldt: LocalDateTime, val newProp1: String? = null)

@Serializable
data class BeanLocalDate(@Contextual val ld: LocalDate, val newProp1: String? = null)

@Serializable
data class BeanBigDecimal(@Contextual val amount: BigDecimal)
