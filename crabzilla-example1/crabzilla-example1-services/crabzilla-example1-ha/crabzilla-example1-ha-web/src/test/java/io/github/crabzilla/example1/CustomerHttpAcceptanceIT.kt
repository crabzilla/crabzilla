package io.github.crabzilla.example1

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import com.jayway.restassured.RestAssured
import com.jayway.restassured.RestAssured.given
import com.jayway.restassured.http.ContentType
import com.jayway.restassured.http.ContentType.JSON
import com.palantir.docker.compose.DockerComposeRule
import com.palantir.docker.compose.connection.waiting.HealthChecks.toRespondOverHttp
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.UnitOfWork
import io.github.crabzilla.core.Version
import io.github.crabzilla.example1.customer.ActivateCustomer
import io.github.crabzilla.example1.customer.CreateCustomer
import io.github.crabzilla.example1.customer.CustomerCreated
import io.github.crabzilla.example1.customer.CustomerId
import io.github.crabzilla.vertx.helpers.EndpointsHelper.restEndpoint
import io.vertx.core.json.Json
import io.vertx.core.logging.LoggerFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import java.io.IOException
import java.util.*


class CustomerHttpAcceptanceIT {

  companion object {

    @JvmStatic
    val log = LoggerFactory.getLogger(CustomerHttpAcceptanceIT::class.java.simpleName)

    val LOCATION_HEADER = "Location"
    val ENTITY_NAME = CommandHandlers.CUSTOMER.name

    var mapper: ObjectMapper = Json.prettyMapper

    @JvmStatic
    @ClassRule
    fun docker(): DockerComposeRule {
      return DockerComposeRule.builder()
        .file("../docker-compose.yml")
        .waitingForService("web", toRespondOverHttp(8080) { port -> port.inFormat("http://127.0.0.1:8080/health") })
        .saveLogsTo("../target/dockerComposeRuleTest")
        .build()
    }

  }

  @Before
  @Throws(InterruptedException::class)
  fun configureRestAssured() {

    RestAssured.baseURI = "http://127.0.0.1"
//    RestAssured.port = Integer.getInteger("http.port", 8081)!!
    RestAssured.port = 8080

    log.info("----> RestAssured.port=" + RestAssured.port)
    mapper.registerModule(ParameterNamesModule())
            .registerModule(Jdk8Module())
            .registerModule(JavaTimeModule())
            .registerModule(KotlinModule())
            .enable(SerializationFeature.INDENT_OUTPUT)
  }

  @After
  fun unconfigureRestAssured() {
    RestAssured.reset()
  }

  // tag::create_customer_test[]

  @Test
  @Throws(IOException::class)
  fun createCustomer() {

    val customerId = CustomerId(UUID.randomUUID().toString())
    val createCustomerCmd = CreateCustomer(UUID.randomUUID(), customerId, "customer test")
    val expectedEvent = CustomerCreated(createCustomerCmd.targetId, "customer test")
    val expectedUow = UnitOfWork(UUID.randomUUID(), createCustomerCmd,
      Version(1), listOf<DomainEvent>(expectedEvent))

    val json = mapper.writerFor(CreateCustomer::class.java).writeValueAsString(createCustomerCmd)

    log.info("command=\n" + json)

    val postCmdResponse = given().contentType(JSON).body(json)
            .`when`().post("/" + restEndpoint(ENTITY_NAME) + "/commands")
            .then().extract().response()

    assertThat(postCmdResponse.statusCode()).isEqualTo(201)
    assertThat(postCmdResponse.header(LOCATION_HEADER))
            .isEqualTo(RestAssured.baseURI + ":" + RestAssured.port + "/"
                    + restEndpoint(ENTITY_NAME) + "/commands/"
                    + createCustomerCmd.commandId.toString())

    val getUowResponse = given().contentType(JSON).body(json)
            .`when`().get(postCmdResponse.header(LOCATION_HEADER))
            .then().statusCode(200).contentType(ContentType.JSON)
            .extract().response()

//    log.info("response -------------------")
//    log.info(getUowResponse.asString())

    val uow = mapper.readValue(getUowResponse.asString(), UnitOfWork::class.java)

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
      Version(1), listOf<DomainEvent>(expectedEvent))

    val json = mapper.writerFor(CreateCustomer::class.java).writeValueAsString(createCustomerCmd)

    log.info("command=\n" + json)

    val postCmdResponse = given().contentType(JSON).body(json)
            .`when`().post("/" + restEndpoint(ENTITY_NAME) + "/commands")
            .then().extract().response()

    assertThat(postCmdResponse.statusCode()).isEqualTo(201)
    assertThat(postCmdResponse.header(LOCATION_HEADER))
            .isEqualTo(RestAssured.baseURI + ":" + RestAssured.port + "/"
                    + restEndpoint(ENTITY_NAME)
                    + "/commands/" + createCustomerCmd.commandId.toString())

    val getUowResponse = given().contentType(JSON).body(json)
            .`when`().get(postCmdResponse.header(LOCATION_HEADER))
            .then().statusCode(200).contentType(ContentType.JSON)
            .extract().response()

//    log.info("response -------------------")
//    log.info(getUowResponse.asString())

    val uow = mapper.readValue(getUowResponse.asString(), UnitOfWork::class.java)

    assertThat(uow.targetId()).isEqualTo(expectedUow.targetId())
    assertThat(uow.command).isEqualTo(expectedUow.command)
    assertThat(uow.events).isEqualTo(expectedUow.events)
    assertThat(uow.version).isEqualTo(expectedUow.version)

    // now lets post it again

    val postCmdResponse2 = given().contentType(JSON).body(json).
            `when`().post("/" + restEndpoint(ENTITY_NAME) + "/commands")
            .then().extract().response()

    assertThat(postCmdResponse2.statusCode()).isEqualTo(201)
    assertThat(postCmdResponse2.header(LOCATION_HEADER))
            .isEqualTo(RestAssured.baseURI + ":" + RestAssured.port + "/"
            + restEndpoint(ENTITY_NAME)
                    + "/commands/" + createCustomerCmd.commandId.toString())

    val getUowResponse2 = given().contentType(JSON).body(json)
            .`when`().get(postCmdResponse2.header(LOCATION_HEADER))
            .then().statusCode(200).contentType(ContentType.JSON)
            .extract().response().asString()

    val uow2 = mapper.readValue(getUowResponse2, UnitOfWork::class.java)

//    log.info("response 2-------------------")
//    log.info(getUowResponse2.toString())

    assertThat(uow).isEqualTo(uow2)
  }

  @Test
  @Throws(IOException::class)
  fun unknownCustomer() {

    val customerId = CustomerId(UUID.randomUUID().toString())
    val activateCustomer = ActivateCustomer(UUID.randomUUID(), customerId, "customer test")
    val json = mapper.writerFor(ActivateCustomer::class.java).writeValueAsString(activateCustomer)

    given().contentType(JSON).body(json)
            .`when`().put("/" + restEndpoint(ENTITY_NAME) + "/commands")
            .then().statusCode(404)

  }

}
