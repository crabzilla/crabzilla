package io.github.crabzilla.example1

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import io.github.crabzilla.example1.customer.CreateCustomer
import io.github.crabzilla.example1.customer.CustomerActivated
import io.github.crabzilla.example1.customer.CustomerCreated
import io.github.crabzilla.example1.customer.CustomerId
import io.github.crabzilla.model.Command
import io.github.crabzilla.model.DomainEvent
import io.github.crabzilla.model.EntityUnitOfWork
import io.github.crabzilla.model.Version
import io.vertx.core.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*
import java.util.Arrays.asList


class JacksonJsonTest {

  internal var mapper = Json.mapper

  private val listOfEventsType = object : TypeReference<List<DomainEvent>>() {

  }

  @BeforeEach
  fun setup() {
    mapper.registerModule(ParameterNamesModule())
            .registerModule(Jdk8Module())
            .registerModule(JavaTimeModule())
            .registerModule(KotlinModule())
  }

  @Test
  @Throws(Exception::class)
  fun one_event() {

    val id = CustomerId(UUID.randomUUID().toString())
    val command = CreateCustomer(UUID.randomUUID(), id, "customer1")
    val event = CustomerCreated(id, command.name)
    val uow1 = EntityUnitOfWork(UUID.randomUUID(), command, Version(1), listOf<DomainEvent>(event))

    val uowAsJson = mapper.writeValueAsString(uow1)

    println(mapper.writerFor(Command::class.java).writeValueAsString(command))
    println(uowAsJson)

    val uow2 = mapper.readValue(uowAsJson, EntityUnitOfWork::class.java)

    assertThat(uow2).isEqualTo(uow1)

  }

  @Test
  @Throws(Exception::class)
  fun more_events() {

    val id = CustomerId("customer#1")
    val command = CreateCustomer(UUID.randomUUID(), id, "customer1")
    val event1 = CustomerCreated(id, command.name)
    val event2 = CustomerActivated("a rgood reason", Instant.now())

    val uow1 = EntityUnitOfWork(UUID.randomUUID(), command, Version(1), asList(event1, event2))

    val uowAsJson = mapper.writeValueAsString(uow1)

    println(uowAsJson)

    val uow2 = mapper.readValue(uowAsJson, EntityUnitOfWork::class.java)

    assertThat(uow2).isEqualTo(uow1)

  }

  @Test
  @Throws(Exception::class)
  fun listOfEvents() {

    val id = CustomerId("customer#1")
    val (_, _, name) = CreateCustomer(UUID.randomUUID(), id, "customer1")
    val event1 = CustomerCreated(id, name)
    val event2 = CustomerActivated("a rgood reason", Instant.now())

    val listOfEvents = Arrays.asList(event1, event2)

    val listOfEventsAsJson = mapper.writerFor(listOfEventsType).writeValueAsString(listOfEvents)

    println(listOfEventsAsJson)

    val listOfEventsFromJson = mapper.readValue<List<DomainEvent>>(listOfEventsAsJson, listOfEventsType)

    assertThat(listOfEventsFromJson).isEqualTo(listOfEvents)

  }


}
