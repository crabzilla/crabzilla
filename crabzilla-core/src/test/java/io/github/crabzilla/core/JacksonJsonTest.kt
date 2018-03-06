package io.github.crabzilla.core

import com.fasterxml.jackson.databind.SerializationFeature
import io.github.crabzilla.example1.customer.CreateCustomer
import io.github.crabzilla.example1.customer.CustomerActivated
import io.github.crabzilla.example1.customer.CustomerCreated
import io.github.crabzilla.example1.customer.CustomerId
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.*
import java.util.Arrays.asList

class JacksonJsonTest {

  companion object {
    private val mapper = ObjectMapperFactory.mapper()
  }

  @Before
  fun setUp() {
    mapper.enable(SerializationFeature.INDENT_OUTPUT)
  }

  @Test
  fun commandToJsonMustWork() {

    val id = CustomerId(UUID.randomUUID().toString())
    val command = CreateCustomer(UUID.randomUUID(), id, id.stringValue())

    val cmdAsJson = commandToJson(mapper, command)

    println(cmdAsJson)

    assertThat(mapper.readValue(cmdAsJson, CreateCustomer::class.java)).isEqualTo(command)

  }

  @Test
  @Throws(Exception::class)
  fun listOfEventsToJsonMustWork() {

    val id = CustomerId(UUID.randomUUID().toString())
    val command = CreateCustomer(UUID.randomUUID(), id, "customer1")

    val event1 = CustomerCreated(id, command.name)
    val event2 = CustomerActivated("a good reason", Instant.now())

    val eventsList = asList(event1, event2)

    val asJson = listOfEventsToJson(mapper, eventsList)

    assertThat(mapper.readerFor(eventsListType).readValue<List<DomainEvent>>(asJson)).isEqualTo(eventsList)

  }

  @Test
  fun listOfEventsFromJsonMustWork() {

    val id = CustomerId(UUID.randomUUID().toString())
    val command = CreateCustomer(UUID.randomUUID(), id, "customer1")

    val event1 = CustomerCreated(id, command.name)
    val event2 = CustomerActivated("a good reason", Instant.now())

    val eventsList = asList(event1, event2)

    val asJson = listOfEventsToJson(mapper, eventsList)

    assertThat(listOfEventsFromJson(mapper, asJson)).isEqualTo(eventsList)

  }

}
