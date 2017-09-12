package io.github.crabzilla.example1

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import com.jayway.restassured.RestAssured
import com.jayway.restassured.RestAssured.given
import com.jayway.restassured.http.ContentType
import com.jayway.restassured.http.ContentType.JSON
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.entity.EntityUnitOfWork
import io.github.crabzilla.core.entity.Version
import io.github.crabzilla.example1.customer.*
import io.github.crabzilla.vertx.entity.EntityCommandExecution
import io.github.crabzilla.vertx.entity.EntityCommandExecution.RESULT.HANDLING_ERROR
import io.github.crabzilla.vertx.entity.EntityCommandExecution.RESULT.SUCCESS
import io.github.crabzilla.vertx.helpers.StringHelper.aggregateRootId
import io.vertx.core.json.Json
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.*

class Example1AcceptanceIT {

  internal var mapper: ObjectMapper = Json.prettyMapper

  @Before
  @Throws(InterruptedException::class)
  fun configureRestAssured() {
    RestAssured.baseURI = "http://localhost"
    RestAssured.port = Integer.getInteger("http.port", 8080)!!
    log.info("----> RestAssured.port=" + RestAssured.port)
    mapper.registerModule(ParameterNamesModule())
            .registerModule(Jdk8Module())
            .registerModule(JavaTimeModule())
            .registerModule(KotlinModule())
  }

  @After
  fun unconfigureRestAssured() {
    RestAssured.reset()
  }

  // tag::create_customer_test[]

  @Test
  @Throws(IOException::class)
  fun successScenario() {

    val customerId = CustomerId(UUID.randomUUID().toString())
    val createCustomerCmd = CreateCustomer(UUID.randomUUID(), customerId, "customer test")
    val expectedEvent = CustomerCreated(createCustomerCmd._targetId, "customer test")
    val expectedUow = EntityUnitOfWork(UUID.randomUUID(), createCustomerCmd,
            Version(1), listOf<DomainEvent>(expectedEvent))

    val json = mapper.writerFor(CreateCustomer::class.java).writeValueAsString(createCustomerCmd)

    val response = given().contentType(JSON).body(json)
                  .`when`().put("/" + aggregateRootId(Customer::class.java) + "/commands")
                  .then().statusCode(201).contentType(ContentType.JSON).extract().response().asString()

    val result = mapper.readValue(response, EntityCommandExecution::class.java)

    assertThat(result.result).isEqualTo(SUCCESS)
    assertThat(result.commandId).isEqualTo(createCustomerCmd.commandId)
    assertThat(result.constraints.isEmpty())

    val uow = result.unitOfWork

    assertThat(uow.targetId()).isEqualTo(expectedUow.targetId())
    assertThat(uow.command).isEqualTo(expectedUow.command)
    assertThat(uow.events).isEqualTo(expectedUow.events)
    assertThat(uow.version).isEqualTo(expectedUow.version)

  }

  // end::create_customer_test[]

  @Test
  @Throws(IOException::class)
  fun handlingErrorScenario() {

    val customerId = CustomerId(UUID.randomUUID().toString())
    val activateCustomer = ActivateCustomer(UUID.randomUUID(), customerId, "customer test")
    val json = mapper.writerFor(ActivateCustomer::class.java).writeValueAsString(activateCustomer)

    val response = given().contentType(JSON).body(json)
            .`when`().put("/" + aggregateRootId(Customer::class.java) + "/commands")
            .then().statusCode(400).contentType(ContentType.JSON).extract().response().asString()

    val result = mapper.readValue(response, EntityCommandExecution::class.java)

    assertThat(result.result).isEqualTo(HANDLING_ERROR)
    assertThat(result.commandId).isEqualTo(activateCustomer.commandId)
    assertThat(result.constraints.isEmpty())

    val uow = result.unitOfWork

    assertThat(uow).isNull()

  }

  companion object {

    val log = KotlinLogging.logger {}

  }

}