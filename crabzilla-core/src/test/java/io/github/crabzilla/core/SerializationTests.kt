package io.github.crabzilla.core

import io.github.crabzilla.example1.Customer
import io.github.crabzilla.example1.CustomerCommand
import io.github.crabzilla.example1.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.CustomerEvent.CustomerRegistered
import io.github.crabzilla.example1.customerJson
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
      {"type":"Customer","id":"${aggregate.id}","name":"${aggregate.name}"}""".trimIndent()
    assertThat(aggregate.toJson(customerJson)).isEqualTo(expectedJson)
    assertThat(AggregateRoot.fromJson<Customer>(customerJson, expectedJson)).isEqualTo(aggregate)
  }

  @Test
  @DisplayName("Command ser/der")
  fun testCmd() {
    val command = RegisterCustomer(customerId = UUID.randomUUID(), name = "name1")
    val expectedJson = """
      {"type":"RegisterCustomer","customerId":"${command.customerId}","name":"${command.name}"}""".trimIndent()
    assertThat(command.toJson(customerJson)).isEqualTo(expectedJson)
    assertThat(Command.fromJson<RegisterCustomer>(customerJson, expectedJson)).isEqualTo(command)
  }

  @Test
  @DisplayName("Event ser/der")
  fun testEvent() {
    val event = CustomerRegistered(UUID.randomUUID(), name = "name1")
    val expectedJson = """
      {"type":"CustomerRegistered","id":"${event.id}","name":"${event.name}"}""".trimIndent()
    assertThat(event.toJson(customerJson)).isEqualTo(expectedJson)
    assertThat(DomainEvent.fromJson<CustomerRegistered>(customerJson, expectedJson)).isEqualTo(event)
  }

  @Test
  @DisplayName("DateTime ser/der")
  fun testLd() {
    val t = Bean2(LocalDate.now())
    val expectedJson = """{"ld":"${t.ld}"}"""
    assertThat(customerJson.encodeToString(t)).isEqualTo(expectedJson)
    assertThat(customerJson.decodeFromString<Bean2>(expectedJson)).isEqualTo(t)
  }

  @Test
  @DisplayName("LocalDateTime ser/der")
  fun testLdt() {
    val t = Bean1(LocalDateTime.now(), "foo")
    val expectedJson = """{"ldt":"${t.ldt}","newProp1":"foo"}"""
    assertThat(customerJson.encodeToString(t)).isEqualTo(expectedJson)
    assertThat(customerJson.decodeFromString<Bean1>(expectedJson)).isEqualTo(t)
  }

  @Test
  @DisplayName("Value Object ser/der")
  fun testVo() {
    val t = Bean3(AValueObject("test", 22), "foo")
    val expectedJson = """{"vo":{"x":"test","y":22},"newProp1":"foo"}"""
    assertThat(customerJson.encodeToString(t)).isEqualTo(expectedJson)
    assertThat(customerJson.decodeFromString<Bean3>(expectedJson)).isEqualTo(t)
  }

  @Test
  @DisplayName("List ser/der")
  fun testx() {
    val c1 = RegisterCustomer(customerId = UUID.randomUUID(), name = "name1")
    val c2 = CustomerCommand.ActivateCustomer("ya")
    val list = listOf(c1, c2)
    val expectedJson = """[{"type":"RegisterCustomer","customerId":"${c1.customerId}",
      "name":"${c1.name}"},{"type":"ActivateCustomer","reason":"${c2.reason}"}]"""
    assertThat(customerJson.encodeToString(list)).isEqualToIgnoringWhitespace(expectedJson)
    assertThat(customerJson.decodeFromString<List<Command>>(expectedJson)).isEqualTo(list)
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
