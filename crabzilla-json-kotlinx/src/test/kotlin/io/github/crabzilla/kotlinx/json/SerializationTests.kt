package io.github.crabzilla.kotlinx.json

import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.CustomerEvent.CustomerRegistered
import kotlinx.serialization.Contextual
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class SerializationTests {
  /**
   * kotlinx.serialization
   */
  @kotlinx.serialization.ExperimentalSerializationApi
  val customerModule =
    SerializersModule {
      include(javaModule)
      polymorphic(CustomerCommand::class) {
        subclass(RegisterCustomer::class, RegisterCustomer.serializer())
        subclass(ActivateCustomer::class, ActivateCustomer.serializer())
        subclass(CustomerCommand.DeactivateCustomer::class, CustomerCommand.DeactivateCustomer.serializer())
        subclass(CustomerCommand.RegisterAndActivateCustomer::class, CustomerCommand.RegisterAndActivateCustomer.serializer())
      }
      polymorphic(CustomerEvent::class) {
        subclass(CustomerRegistered::class, CustomerRegistered.serializer())
        subclass(CustomerEvent.CustomerActivated::class, CustomerEvent.CustomerActivated.serializer())
        subclass(CustomerEvent.CustomerDeactivated::class, CustomerEvent.CustomerDeactivated.serializer())
      }
    }

  val example1Json = Json { serializersModule = customerModule }
  val commandSerDer = PolymorphicSerializer(CustomerCommand::class)
  val eventSerDer = PolymorphicSerializer(CustomerEvent::class)

  val json = Json { serializersModule = javaModule }

  @Test
  @DisplayName("Command ser/der")
  fun testCmdxxx() {
    val command = RegisterCustomer(customerId = UUID.randomUUID(), name = "name1")
    val expectedJson =
      """
      {"customerId":"${command.customerId}","name":"${command.name}"}
      """.trimIndent()
    assertThat(json.encodeToString(command)).isEqualTo(expectedJson)
    assertThat(example1Json.decodeFromString<RegisterCustomer>(expectedJson)).isEqualTo(command)
  }

  @Test
  @DisplayName("Command ser/der")
  fun testCmd() {
    val command = RegisterCustomer(customerId = UUID.randomUUID(), name = "name1")
    val expectedJson =
      """
      {"type":"RegisterCustomer","customerId":"${command.customerId}","name":"${command.name}"}
      """.trimIndent()
    assertThat(example1Json.encodeToString(commandSerDer, command)).isEqualTo(expectedJson)
    assertThat(example1Json.decodeFromString(commandSerDer, expectedJson)).isEqualTo(command)
  }

  @Test
  @DisplayName("Event ser/der")
  fun testEvent() {
    val event = CustomerRegistered(UUID.randomUUID(), name = "name1")
    val expectedJson =
      """
      {"type":"CustomerRegistered","id":"${event.id}","name":"${event.name}"}
      """.trimIndent()
    assertThat(example1Json.encodeToString(eventSerDer, event)).isEqualTo(expectedJson)
    assertThat(example1Json.decodeFromString(eventSerDer, expectedJson)).isEqualTo(event)
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

  //  @RepeatedTest(100)
  @Test
  @DisplayName("LocalDateTime ser/der")
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
data class BeanValueObject(
  @Contextual val vo: AValueObject,
  val newProp1: String? = null,
)

@Serializable
data class BeanLocalDateTime(
  @Contextual val ldt: LocalDateTime,
  val newProp1: String? = null,
)

@Serializable
data class BeanLocalDate(
  @Contextual val ld: LocalDate,
  val newProp1: String? = null,
)

@Serializable
data class BeanBigDecimal(
  @Contextual val amount: BigDecimal,
)
