package io.github.crabzilla

import com.fasterxml.jackson.databind.SerializationFeature
import io.github.crabzilla.example1.customer.CreateCustomer
import io.github.crabzilla.example1.customer.CustomerActivated
import io.github.crabzilla.example1.customer.CustomerCreated
import io.github.crabzilla.example1.customer.CustomerId
import io.github.crabzilla.vertx.initVertx
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*
import java.util.Arrays.asList

class JacksonJsonTest {

  lateinit var vertx: Vertx

  @BeforeEach
  fun setUp() {
    vertx = Vertx.vertx()
    initVertx(vertx)
    Json.mapper.enable(SerializationFeature.INDENT_OUTPUT)
  }

  @Test
  fun commandToJsonMustWork() {

    val id = CustomerId(1)
    val command = CreateCustomer(UUID.randomUUID(), id, "cust-1")

    val cmdAsJson = commandToJson(Json.mapper, command)

    assertThat(Json.mapper.readValue(cmdAsJson, CreateCustomer::class.java)).isEqualTo(command)

  }

  @Test
  fun commandFromJsonMustWork() {

    val id = CustomerId(1)
    val command = CreateCustomer(UUID.randomUUID(), id, "cust-1")

    val cmdAsJson = commandToJson(Json.mapper, command)
    val commandFromJson = commandFromJson(Json.mapper, cmdAsJson)

    assertThat(command).isEqualTo(commandFromJson)

  }

  @Test
  @Throws(Exception::class)
  fun listOfEventsToJsonMustWork() {

    val id = CustomerId(1)
    val command = CreateCustomer(UUID.randomUUID(), id, "cust-1")

    val event1 = CustomerCreated(id, command.name)
    val event2 = CustomerActivated("a good reason", Instant.now())

    val eventsList = asList(event1, event2)

    val asJson = listOfEventsToJson(Json.mapper, eventsList)

    assertThat(Json.mapper.readerFor(eventsListType).readValue<List<DomainEvent>>(asJson)).isEqualTo(eventsList)

  }

  @Test
  fun listOfEventsFromJsonMustWork() {

    val id = CustomerId(1)
    val command = CreateCustomer(UUID.randomUUID(), id, "cust-1")

    val event1 = CustomerCreated(id, command.name)
    val event2 = CustomerActivated("a good reason", Instant.now())

    val eventsList = asList(event1, event2)

    val asJson = listOfEventsToJson(Json.mapper, eventsList)

    assertThat(listOfEventsFromJson(Json.mapper, asJson)).isEqualTo(eventsList)

  }

}
