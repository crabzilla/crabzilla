package io.github.crabzilla.example1

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import io.github.crabzilla.DomainEvent
import io.github.crabzilla.UnitOfWork
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
    val command = CreateCustomer(UUID.randomUUID(), id, id.stringValue())

    val cmdAsJson = mapper.writerFor(CreateCustomer::class.java).writeValueAsString(command)

    assertThat(mapper.readValue(cmdAsJson, CreateCustomer::class.java)).isEqualTo(command)

  }

  @Test
  @Throws(Exception::class)
  fun oneEvent() {

    val id = CustomerId(UUID.randomUUID().toString())
    val command = CreateCustomer(UUID.randomUUID(), id, "customer " + id)
    val event = CustomerCreated(id, command.name)
    val uow1 = UnitOfWork(UUID.randomUUID(), command, 1, listOf<DomainEvent>(event))

    val uowAsJson = mapper.writeValueAsString(uow1)

    val uow2 = mapper.readValue(uowAsJson, UnitOfWork::class.java)

    assertThat(uow2).isEqualTo(uow1)

  }

  @Test
  @Throws(Exception::class)
  fun moreEvents() {

    val id = CustomerId(UUID.randomUUID().toString())
    val command = CreateCustomer(UUID.randomUUID(), id, "customer1")

    val event1 = CustomerCreated(id, command.name)
    val event2 = CustomerActivated("a good reason", Instant.now())

    val uow1 = UnitOfWork(UUID.randomUUID(), command, 1, asList<DomainEvent>(event1, event2))

    val uowAsJson = mapper.writeValueAsString(uow1)

    val uow2 = mapper.readValue(uowAsJson, UnitOfWork::class.java)

    assertThat(uow2).isEqualTo(uow1)

  }

}
