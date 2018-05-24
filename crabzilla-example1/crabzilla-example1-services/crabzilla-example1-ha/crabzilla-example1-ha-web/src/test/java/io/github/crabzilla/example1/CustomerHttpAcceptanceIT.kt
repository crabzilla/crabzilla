package io.github.crabzilla.example1

import com.palantir.docker.compose.DockerComposeRule
import com.palantir.docker.compose.connection.waiting.HealthChecks.toRespondOverHttp
import io.github.crabzilla.core.*
import io.github.crabzilla.example1.customer.ActivateCustomer
import io.github.crabzilla.example1.customer.CreateCustomer
import io.github.crabzilla.example1.customer.CustomerCreated
import io.github.crabzilla.example1.customer.CustomerId
import io.github.crabzilla.vertx.helpers.EndpointsHelper.restEndpoint
import org.assertj.core.api.Assertions.assertThat
import org.junit.ClassRule
import org.junit.Test
import java.io.IOException
import java.util.*


class CustomerHttpAcceptanceIT {

  companion object {

    val ENTITY_NAME = CommandHandlers.CUSTOMER.name

    const val LOCATION_HEADER = "Location"
    const val port = 8080
    const val baseURI = "http://127.0.0.1"

    val mapper = ObjectMapperFactory.mapper()

    val client = RetrofitClientFactory.create("$baseURI:$port", CrabzillaRestApi::class.java, mapper)

    @JvmStatic
    @ClassRule
    fun docker(): DockerComposeRule {
      return DockerComposeRule.builder()
        .file("../docker-compose.yml")
        .waitingForService("web", toRespondOverHttp(port) { port -> port.inFormat("http://127.0.0.1:8080/health") })
        .waitingForService("command-handler", toRespondOverHttp(8081) { port -> port.inFormat("http://127.0.0.1:8081/health") })
        .waitingForService("events-projector", toRespondOverHttp(8082) { port -> port.inFormat("http://127.0.0.1:8082/health") })
        .saveLogsTo("../target/dockerComposeRuleTest")
        .build()
    }

  }

  // tag::create_customer_test[]

  @Test
  @Throws(IOException::class)
  fun createCustomer() {

    val customerId = CustomerId(UUID.randomUUID().toString())
    val createCustomerCmd = CreateCustomer(UUID.randomUUID(), customerId, "customer test")
    val expectedEvent = CustomerCreated(createCustomerCmd.targetId, "customer test")
    val expectedUow = UnitOfWork(UUID.randomUUID(), createCustomerCmd,
      1, listOf<DomainEvent>(expectedEvent))

    val postCall = client.postCommand(restEndpoint(ENTITY_NAME), createCustomerCmd)
    val postCmdResponse = postCall.execute()

    assertThat(postCmdResponse.code()).isEqualTo(201)
    assertThat(postCmdResponse.headers()[LOCATION_HEADER])
            .isEqualTo("$baseURI:$port/"
                    + restEndpoint(ENTITY_NAME) + "/commands/"
                    + createCustomerCmd.commandId.toString())

    val getCall = client.getUnitOfWork(restEndpoint(ENTITY_NAME), createCustomerCmd.commandId.toString())
    val getUowResponse = getCall.execute()

    println("response -------------------")
    println(getUowResponse.body())

    val uow = getUowResponse.body()!!

    assertThat(uow.targetId()).isEqualTo(expectedUow.targetId())
    assertThat(uow.command).isEqualTo(expectedUow.command)
    assertThat(uow.events).isEqualTo(expectedUow.events)
    assertThat(uow.version).isEqualTo(expectedUow.version)

  }

  // end::create_customer_test[]

  @Test
  @Throws(IOException::class)
  fun createCustomerIdempotency() {

    val customerId = CustomerId(UUID.randomUUID().toString())
    val createCustomerCmd = CreateCustomer(UUID.randomUUID(), customerId, "customer test")
    val expectedEvent = CustomerCreated(createCustomerCmd.targetId, "customer test")
    val expectedUow = UnitOfWork(UUID.randomUUID(), createCustomerCmd,
      1, listOf<DomainEvent>(expectedEvent))

    val postCall = client.postCommand(restEndpoint(ENTITY_NAME), createCustomerCmd)
    val postCmdResponse = postCall.execute()

    assertThat(postCmdResponse.code()).isEqualTo(201)
    assertThat(postCmdResponse.headers()[LOCATION_HEADER])
      .isEqualTo("$baseURI:$port/"
        + restEndpoint(ENTITY_NAME) + "/commands/"
        + createCustomerCmd.commandId.toString())

    val getCall = client.getUnitOfWork(restEndpoint(ENTITY_NAME), createCustomerCmd.commandId.toString())
    val getUowResponse = getCall.execute()

    println("response -------------------")
    println(getUowResponse.body())

    val uow = getUowResponse.body()!!

    assertThat(uow.targetId()).isEqualTo(expectedUow.targetId())
    assertThat(uow.command).isEqualTo(expectedUow.command)
    assertThat(uow.events).isEqualTo(expectedUow.events)
    assertThat(uow.version).isEqualTo(expectedUow.version)

    // now lets post it again

    val postCall2 = client.postCommand(restEndpoint(ENTITY_NAME), createCustomerCmd)
    val postCmdResponse2 = postCall2.execute()

    assertThat(postCmdResponse2.code()).isEqualTo(201)
    assertThat(postCmdResponse2.headers()[LOCATION_HEADER])
      .isEqualTo("$baseURI:$port/"
        + restEndpoint(ENTITY_NAME) + "/commands/"
        + createCustomerCmd.commandId.toString())

    val getCall2 = client.getUnitOfWork(restEndpoint(ENTITY_NAME), createCustomerCmd.commandId.toString())
    val getUowResponse2 = getCall2.execute()

    println("response -------------------")
    println(getUowResponse2.body())

    val uow2 = getUowResponse2.body()!!

    assertThat(uow2.targetId()).isEqualTo(expectedUow.targetId())
    assertThat(uow2.command).isEqualTo(expectedUow.command)
    assertThat(uow2.events).isEqualTo(expectedUow.events)
    assertThat(uow2.version).isEqualTo(expectedUow.version)

    assertThat(uow).isEqualTo(uow2)
  }

  @Test
  @Throws(IOException::class)
  fun unknownCustomer() {

    val customerId = CustomerId(UUID.randomUUID().toString())
    val activateCustomer = ActivateCustomer(UUID.randomUUID(), customerId, "customer test")

    println(activateCustomer)

    val postCall = client.postCommand(restEndpoint(ENTITY_NAME), activateCustomer)
    val postCmdResponse = postCall.execute()

    assertThat(400).isEqualTo(postCmdResponse.code())
    // TODO
    // assertThat(postCmdResponse.body()).isEqualTo(listOf("customer must exists"))

  }

}

