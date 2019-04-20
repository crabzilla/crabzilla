package io.github.crabzilla

import io.vertx.core.Vertx
import org.junit.jupiter.api.BeforeEach

class JacksonJsonTest {

  lateinit var vertx: Vertx

  @BeforeEach
  fun setUp() {
    vertx = Vertx.vertx()
    initVertx(vertx)
  }

//  @Test
//  fun commandToJsonMustWork() {
//
//    val id = CustomerId(1)
//    val command = CreateCustomer(UUID.randomUUID(), id, "cust-1")
//
//    val cmdAsJson = commandToJson(command)
//
//    assertThat(Json.mapper.readValue(cmdAsJson, CreateCustomer::class.java)).isEqualTo(command)
//
//  }
//
//  @Test
//  fun commandFromJsonMustWork() {
//
//    val id = CustomerId(1)
//    val command = CreateCustomer(UUID.randomUUID(), id, "cust-1")
//
//    val cmdAsJson = commandToJson(command)
//    val commandFromJson = commandFromJson(cmdAsJson)
//
//    assertThat(command).isEqualTo(commandFromJson)
//
//  }
//
//  @Test
//  @Throws(Exception::class)
//  fun listOfEventsToJsonMustWork() {
//
//    val id = CustomerId(1)
//    val command = CreateCustomer(UUID.randomUUID(), id, "cust-1")
//
//    val event1 = CustomerCreated(id, command.name)
//    val event2 = CustomerActivated("a good reason", Instant.now())
//
//    val eventsList = asList(event1, event2)
//
//    val asJson = listOfEventsToJson(eventsList)
//
//    assertThat(Json.mapper.readerFor(eventsListType).readValue<List<DomainEvent>>(asJson)).isEqualTo(eventsList)
//
//    println(asJson)
//
//  }
//
//  @Test
//  fun listOfEventsFromJsonMustWork() {
//
//    val id = CustomerId(1)
//    val command = CreateCustomer(UUID.randomUUID(), id, "cust-1")
//
//    val event1 = CustomerCreated(id, command.name)
//    val event2 = CustomerActivated("a good reason", Instant.now())
//
//    val eventsList = asList(event1, event2)
//
//    val asJson = listOfEventsToJson(eventsList)
//
//    assertThat(listOfEventsFromJson(asJson)).isEqualTo(eventsList)
//
//  }

}
