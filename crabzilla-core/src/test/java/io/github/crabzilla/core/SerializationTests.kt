package io.github.crabzilla.core

import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRegistered
import io.github.crabzilla.example1.example1Json
import io.github.crabzilla.example1.payment.Payment
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SerializationTests {

  @Test
  @DisplayName("Aggregate ser/der")
  fun testAggr() {
    val aggregate = Customer(UUID.randomUUID(), "name1")
    val expectedJson = """
      {"type":"Customer","id":"${aggregate.id}","name":"${aggregate.name}"}
    """.trimIndent()
    assertThat(aggregate.toJson(example1Json)).isEqualTo(expectedJson)
    assertThat(State.fromJson<Customer>(example1Json, expectedJson)).isEqualTo(aggregate)
  }

  @Test
  @DisplayName("Command ser/der")
  fun testCmd() {
    val command = RegisterCustomer(customerId = UUID.randomUUID(), name = "name1")
    val expectedJson = """
      {"type":"RegisterCustomer","customerId":"${command.customerId}","name":"${command.name}"}
    """.trimIndent()
    assertThat(command.toJson(example1Json)).isEqualTo(expectedJson)
    assertThat(Command.fromJson<RegisterCustomer>(example1Json, expectedJson)).isEqualTo(command)
  }

  @Test
  @DisplayName("Event ser/der")
  fun testEvent() {
    val event = CustomerRegistered(UUID.randomUUID(), name = "name1")
    val expectedJson = """
      {"type":"CustomerRegistered","id":"${event.id}","name":"${event.name}"}
    """.trimIndent()
    assertThat(event.toJson(example1Json)).isEqualTo(expectedJson)
    assertThat(Event.fromJson<CustomerRegistered>(example1Json, expectedJson)).isEqualTo(event)
  }

  @Test
  @DisplayName("DateTime ser/der")
  fun testLd() {
    val t = Bean2(LocalDate.now())
    val expectedJson = """{"ld":"${t.ld}"}"""
    assertThat(example1Json.decodeFromString<Bean2>(expectedJson)).isEqualTo(t)
    val resultJson = example1Json.encodeToString(t)
    assertThat(example1Json.decodeFromString<Bean2>(resultJson)).isEqualTo(t)
  }

  @Test
  @DisplayName("LocalDateTime ser/der")
//  @RepeatedTest(100)
  fun testLdt() {
    val t = Bean1(LocalDateTime.now(), "foo")
    val expectedJson = """{"ldt":"${t.ldt}","newProp1":"foo"}"""
    assertThat(example1Json.encodeToString(t)).startsWith(expectedJson)
    assertThat(example1Json.decodeFromString<Bean1>(expectedJson)).isEqualTo(t)
  }

  @Test
  @DisplayName("Value Object ser/der")
  fun testVo() {
    val t = Bean3(AValueObject("test", 22), "foo")
    val expectedJson = """{"vo":{"x":"test","y":22},"newProp1":"foo"}"""
    assertThat(example1Json.encodeToString(t)).isEqualTo(expectedJson)
    assertThat(example1Json.decodeFromString<Bean3>(expectedJson)).isEqualTo(t)
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
    assertThat(example1Json.decodeFromString<List<Command>>(expectedJson)).isEqualTo(list)
  }

  @Test
  @DisplayName("Event List ser/der")
  fun testEventList() {
    val event1 = CustomerRegistered(UUID.randomUUID(), name = "name1")
    val event2 = CustomerEvent.CustomerActivated("because yes")
    val asJson = example1Json.encodeToString(listOf(event1, event2))
    assertThat(example1Json.decodeFromString<List<Event>>(asJson)).isEqualTo(listOf(event1, event2))
  }

  @Test
  @DisplayName("State List ser/der")
  fun testStateList() {
    val state1 = Customer(UUID.randomUUID(), name = "name1")
    val state2 = Payment(UUID.randomUUID(), "because yes", 10.00)
    val asJson = example1Json.encodeToString(listOf(state1, state2))
    assertThat(example1Json.decodeFromString<List<State>>(asJson)).isEqualTo(listOf(state1, state2))
  }
}

@Serializable
data class AValueObject(val x: String, val y: Int)

@Serializable
data class Bean1(@Contextual val ldt: LocalDateTime, val newProp1: String? = null)

@Serializable
data class Bean2(@Contextual val ld: LocalDate, val newProp1: String? = null)

@Serializable
data class Bean3(@Contextual val vo: AValueObject, val newProp1: String? = null)
