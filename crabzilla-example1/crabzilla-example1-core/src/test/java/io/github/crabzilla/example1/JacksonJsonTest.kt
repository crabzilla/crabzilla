package io.github.crabzilla.example1

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.entity.EntityUnitOfWork
import io.github.crabzilla.core.entity.Version
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

  internal var mapper = ObjectMapper()

  @Before
  fun setUp() {
    mapper.registerModule(ParameterNamesModule())
            .registerModule(Jdk8Module())
            .registerModule(JavaTimeModule())
            .registerModule(KotlinModule())
            .enable(SerializationFeature.INDENT_OUTPUT)
  }

  @Test
  @Throws(Exception::class)
  fun createCommand() {

    val id = CustomerId(UUID.randomUUID().toString())
    val command = CreateCustomer(UUID.randomUUID(), id, "customer1")

    val cmdAsJson = mapper.writerFor(CreateCustomer::class.java).writeValueAsString(command)

    println(cmdAsJson)

    assertThat(mapper.readValue(cmdAsJson, CreateCustomer::class.java)).isEqualTo(command)

  }

  @Test
  @Throws(Exception::class)
  fun oneEvent() {

    val id = CustomerId(UUID.randomUUID().toString())
    val command = CreateCustomer(UUID.randomUUID(), id, "customer1")
    val event = CustomerCreated(id, command.name)
    val uow1 = EntityUnitOfWork(UUID.randomUUID(), command, Version.create(1), listOf<DomainEvent>(event))

    val uowAsJson = mapper.writeValueAsString(uow1)

    val uow2 = mapper.readValue(uowAsJson, EntityUnitOfWork::class.java)

    assertThat(uow2).isEqualTo(uow1)

  }

  @Test
  @Throws(Exception::class)
  fun moreEvents() {

    val id = CustomerId("customer#1")
    val command = CreateCustomer(UUID.randomUUID(), id, "customer1")
    val event1 = CustomerCreated(id, command.name)
    val event2 = CustomerActivated("a rgood reason", Instant.now())

    val uow1 = EntityUnitOfWork(UUID.randomUUID(), command, Version.create(1), asList<DomainEvent>(event1, event2))

    val uowAsJson = mapper.writeValueAsString(uow1)

    System.out.println(uowAsJson);


    System.out.println(uow1);


    val uow2 = mapper.readValue(uowAsJson, EntityUnitOfWork::class.java)

    assertThat(uow2).isEqualTo(uow1)

  }

  val uow1 = """
    {
  "unitOfWorkId" : "7741355a-520b-4f36-a1e4-418a61b1a3f9",
  "command" : {
    "@class" : "io.github.crabzilla.example1.customer.CreateCustomer",
    "commandId" : "49f0f4ca-7c19-46fb-9406-c8a33c2eb971",
    "targetId" : {
      "@class" : "io.github.crabzilla.example1.customer.CustomerId",
      "id" : "fdcf38e4-34a7-46c9-b5de-051e6e19bd74"
    },
    "name" : "customer test"
  },
  "version" : {
    "valueAsLong" : 1
  },
  "events" : [ {
    "@class" : "io.github.crabzilla.example1.customer.CustomerCreated",
    "id" : {
      "@class" : "io.github.crabzilla.example1.customer.CustomerId",
      "id" : "fdcf38e4-34a7-46c9-b5de-051e6e19bd74"
    },
    "name" : "customer test"
  } ]
}
    """
}
