package io.github.crabzilla.example1

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import com.palantir.docker.compose.DockerComposeRule
import com.palantir.docker.compose.connection.waiting.HealthChecks.toRespondOverHttp
import io.github.crabzilla.core.Command
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.UnitOfWork
import io.github.crabzilla.example1.customer.ActivateCustomer
import io.github.crabzilla.example1.customer.CreateCustomer
import io.github.crabzilla.example1.customer.CustomerCreated
import io.github.crabzilla.example1.customer.CustomerId
import io.github.crabzilla.vertx.helpers.EndpointsHelper.restEndpoint
import io.vertx.core.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.ClassRule
import org.junit.Test
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.*
import java.io.IOException
import java.util.*


class CustomerHttpAcceptanceIT {

  companion object {

    val ENTITY_NAME = CommandHandlers.CUSTOMER.name

    const val LOCATION_HEADER = "Location"
    const val port = 8080
    const val baseURI = "http://127.0.0.1"

    var mapper: ObjectMapper = Json.prettyMapper

    val client = create()

    interface Example1Api {

      @Headers("Content-Type: application/json")
      @POST("/{resourceId}/commands")
      fun postCommand(@Path("resourceId") resourceId: String, @Body command: Command): Call<Void>

      @Headers("Content-Type: application/json")
      @GET("/{resourceId}/commands/{commandId}")
      fun getUnitOfWork(@Path("resourceId") resourceId: String, @Path("commandId") commandId: String): Call<UnitOfWork>

    }

    @JvmStatic
    @Throws(Exception::class)
    internal fun create(): Example1Api { // just for test

      mapper.registerModule(ParameterNamesModule())
        .registerModule(Jdk8Module())
        .registerModule(JavaTimeModule())
        .registerModule(KotlinModule())
        .enable(SerializationFeature.INDENT_OUTPUT)

      val retrofit = Retrofit.Builder()
        .baseUrl("$baseURI:$port")
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(JacksonConverterFactory.create(mapper))
        .client(OkHttpClientFactory.getUnsafeOkHttpClient())
        .build()

      return retrofit.create(Example1Api::class.java)

    }

    @JvmStatic
    @ClassRule
    fun docker(): DockerComposeRule {
      return DockerComposeRule.builder()
        .file("../docker-compose.yml")
        .waitingForService("web", toRespondOverHttp(port) { port -> port.inFormat("http://127.0.0.1:8080/health") })
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

    assertThat(postCmdResponse.code()).isEqualTo(500)

  }

}

