package io.github.crabzilla.web.query

import io.github.crabzilla.core.command.UnitOfWork.JsonMetadata.COMMAND
import io.github.crabzilla.core.command.UnitOfWork.JsonMetadata.COMMAND_ID
import io.github.crabzilla.core.command.UnitOfWork.JsonMetadata.ENTITY_ID
import io.github.crabzilla.core.command.UnitOfWork.JsonMetadata.ENTITY_NAME
import io.github.crabzilla.core.command.UnitOfWork.JsonMetadata.EVENTS
import io.github.crabzilla.core.command.UnitOfWork.JsonMetadata.VERSION
import io.github.crabzilla.web.boilerplate.ConfigSupport.getConfig
import io.github.crabzilla.web.boilerplate.DeploySupport.deploy
import io.github.crabzilla.web.boilerplate.HttpSupport.findFreeHttpPort
import io.github.crabzilla.web.example1.cleanDatabase
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpMethod.GET
import io.vertx.core.http.HttpMethod.POST
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.SLF4JLogDelegateFactory
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.ext.web.client.predicate.ResponsePredicate
import io.vertx.ext.web.codec.BodyCodec
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.junit5.web.TestRequest.jsonBodyResponse
import io.vertx.junit5.web.TestRequest.statusCode
import io.vertx.junit5.web.TestRequest.testRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.extension.ExtendWith
import org.junitpioneer.jupiter.RepeatFailedTest
import org.slf4j.LoggerFactory
import java.util.Random
import java.util.UUID
import java.util.function.Consumer

/**
 * Integration test
 */
@ExtendWith(VertxExtension::class)
@TestInstance(Lifecycle.PER_CLASS)
internal class ProjectionFromDbIT {

  val random = Random()
  var nextInt = random.nextInt()
  var customerId2 = random.nextInt()
  private val httpPort = findFreeHttpPort()
  private lateinit var client: WebClient

  companion object {
    private val log = LoggerFactory.getLogger(ProjectionFromDbIT::class.java)
    init {
      System.setProperty(io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME,
        SLF4JLogDelegateFactory::class.java.name)
      LoggerFactory.getLogger(io.vertx.core.logging.LoggerFactory::class.java) // Required for Logback to work in Vertx
    }
  }

  @BeforeAll
  fun setup(tc: VertxTestContext, vertx: Vertx) {
    getConfig(vertx, "./../example1.env")
      .onSuccess { config: JsonObject ->
        config.put("HTTP_PORT", httpPort)
        val deploymentOptions = DeploymentOptions().setConfig(config).setInstances(1)
        val wco = WebClientOptions()
        wco.defaultHost = "0.0.0.0"
        wco.defaultPort = httpPort
        client = WebClient.create(vertx, wco)
        deploy(vertx, ProjectionFromDbVerticle::class.java.name, deploymentOptions)
          .onComplete {
            cleanDatabase(vertx, config)
              .onSuccess { tc.completeNow() }
              .onFailure { tc.failNow(it) }
          }
      }.onFailure { tc.failNow(it) }
  }

  @Nested
  @DisplayName("When sending a valid CreateCommand expecting uow body")
  internal inner class When1 {
    @Test
    @DisplayName("You get a correspondent UnitOfWork as JSON")
    fun a1(tc: VertxTestContext) {
      val cmdAsJson = JsonObject("{\"name\":\"customer#$customerId2\"}")
      val route = "/commands/customer/$customerId2/create"
      log.info("$route with json \n {}", customerId2, cmdAsJson.encodePrettily())
      testRequest(client, POST, route)
        .expect(statusCode(200))
        .expect(Consumer { response: HttpResponse<Buffer?> ->
          val uow = response.bodyAsJsonObject()
          log.info("UnitOfWork {}", uow.encodePrettily())
          val uowId = java.lang.Long.valueOf(response.getHeader("uowId"))
          assertThat(uowId).isPositive()
          assertThat(uow.getString(ENTITY_NAME)).isEqualTo("customer")
          assertThat(uow.getInteger(ENTITY_ID)).isEqualTo(customerId2)
          assertThat(uow.getString(COMMAND_ID)).isNotNull()
          assertThat(uow.getJsonObject(COMMAND)).isEqualTo(cmdAsJson)
          assertThat(uow.getInteger(VERSION)).isEqualTo(1)
          assertThat(uow.getJsonArray(EVENTS).size()).isEqualTo(1)
        })
        .sendJson(cmdAsJson, tc)
    }

    @Test
    @DisplayName("You get a correspondent snapshot (write model)")
    fun a2(tc: VertxTestContext) {
      client["/commands/customer/$customerId2"]
        .`as`(BodyCodec.jsonObject())
        .expect(ResponsePredicate.SC_SUCCESS)
        .expect(ResponsePredicate.JSON)
        .send(tc.succeeding { response2: HttpResponse<JsonObject> ->
          tc.verify {
            val jo = response2.body()
            log.info("UnitOfWork {}", jo.encodePrettily())
            assertThat(jo.getInteger("version")).isEqualTo(1)
            val state = jo.getJsonObject("state")
            assertThat(state.getInteger("customerId")).isEqualTo(customerId2)
            assertThat(state.getString("name")).isEqualTo("customer#$customerId2")
            assertThat(state.getBoolean("isActive")).isFalse()
            tc.completeNow()
          }
        })
    }

    @Test
    @DisplayName("You get a correspondent company summary (read model)")
    @RepeatFailedTest(3)
    fun a3(tc: VertxTestContext?) {
      Thread.sleep(1000) // to wait for projection
      val route = "/customers/$customerId2"
      val expected = JsonObject("{\"id\":$customerId2,\"name\":\"customer#$customerId2\",\"is_active\":false}")
      testRequest(client, GET, route)
        .expect(statusCode(200))
        .expect(jsonBodyResponse(expected))
        .send(tc)
    }

    @Test
    @DisplayName("You get a correspondent entity tracking")
    fun a4(tc: VertxTestContext) {
      client["/commands/customer/$customerId2/units-of-work"]
        .`as`(BodyCodec.jsonArray())
        .expect(ResponsePredicate.SC_SUCCESS)
        .expect(ResponsePredicate.JSON)
        .send(tc.succeeding { response2: HttpResponse<JsonArray> ->
          tc.verify {
            val array = response2.body()
            assertThat(array.size()).isEqualTo(1)
            tc.completeNow()
          }
        })
    }
  }

  @Nested
  @DisplayName("When sending the same idempotent valid CreateCommand 2 times expecting uow body")
  internal inner class When2 {
    var customerId3 = customerId2 + 1

    @Test
    @DisplayName("You get a correspondent UnitOfWork as JSON")
    fun a1(tc: VertxTestContext) {
      val cmdAsJson = JsonObject("{\"name\":\"customer#$customerId3\"}")
      log.info("/commands/customer/{}/create with json \n {}", customerId3, cmdAsJson.encodePrettily())
      val cmdId = UUID.randomUUID()
      client.post("/commands/customer/$customerId3/create")
        .`as`(BodyCodec.jsonObject())
        .putHeader("commandId", cmdId.toString())
        .expect(ResponsePredicate.SC_SUCCESS)
        .expect(ResponsePredicate.JSON)
        .sendJson(cmdAsJson, tc.succeeding { response1: HttpResponse<JsonObject> ->
          tc.verify {
            val uow1 = response1.body()
            log.info("UnitOfWork {}", uow1.encodePrettily())
            val uowId1 = java.lang.Long.valueOf(response1.getHeader("uowId"))
            assertThat(uowId1).isPositive()
            assertThat(uow1.getString(ENTITY_NAME)).isEqualTo("customer")
            assertThat(uow1.getInteger(ENTITY_ID)).isEqualTo(customerId3)
            assertThat(uow1.getString(COMMAND_ID)).isNotNull()
            assertThat(uow1.getJsonObject(COMMAND)).isEqualTo(cmdAsJson)
            assertThat(uow1.getInteger(VERSION)).isEqualTo(1)
            assertThat(uow1.getJsonArray(EVENTS).size()).isEqualTo(1)
            client.post(httpPort, "0.0.0.0", "/commands/customer/$customerId3/create")
              .`as`(BodyCodec.jsonObject())
              .putHeader("commandId", cmdId.toString())
              .expect(ResponsePredicate.SC_SUCCESS)
              .expect(ResponsePredicate.JSON)
              .sendJson(cmdAsJson, tc.succeeding { response2: HttpResponse<JsonObject> ->
                tc.verify {
                  val uow2 = response2.body()
                  val uowId2 = java.lang.Long.valueOf(response2.getHeader("uowId"))
                  assertThat(uow2).isEqualTo(uow1)
                  assertThat(uowId2).isEqualTo(uowId1)
                  tc.completeNow()
                }
              }
              )
          }
        }
        )
    }

    @Test
    @DisplayName("You get a correspondent snapshot (write model)")
    fun a2(tc: VertxTestContext) {
      client["/commands/customer/$customerId3"]
        .`as`(BodyCodec.jsonObject())
        .expect(ResponsePredicate.SC_SUCCESS)
        .expect(ResponsePredicate.JSON)
        .send(tc.succeeding { response2: HttpResponse<JsonObject> ->
          tc.verify {
            val jo = response2.body()
            assertThat(jo.getInteger("version")).isEqualTo(1)
            val state = jo.getJsonObject("state")
            assertThat(state.getInteger("customerId")).isEqualTo(customerId3)
            assertThat(state.getString("name")).isEqualTo("customer#$customerId3")
            assertThat(state.getBoolean("isActive")).isFalse()
            tc.completeNow()
          }
        })
    }

    @Test
    @DisplayName("You get a correspondent company summary (read model)")
    @RepeatFailedTest(3)
    fun a3(tc: VertxTestContext?) {
      Thread.sleep(1000) // to wait for projection
      val route = "/customers/$customerId3"
      val expected = "{\"id\":$customerId3,\"name\":\"customer#$customerId3\",\"is_active\":false}"
      testRequest(client, GET, route)
        .expect(statusCode(200))
        .expect(Consumer { obj: HttpResponse<Buffer?> -> obj.bodyAsJsonObject() })
        .expect(Consumer { bufferHttpResponse: HttpResponse<Buffer?> -> bufferHttpResponse.bodyAsString() == expected })
        .send(tc)
    }

    @Test
    @DisplayName("You get a correspondent entity tracking")
    fun a4(tc: VertxTestContext) {
      client["/commands/customer/$customerId3/units-of-work"]
        .`as`(BodyCodec.jsonArray())
        .expect(ResponsePredicate.SC_SUCCESS)
        .expect(ResponsePredicate.JSON)
        .send(tc.succeeding { response2: HttpResponse<JsonArray> ->
          tc.verify {
            val array = response2.body()
            assertThat(array.size()).isEqualTo(1)
            tc.completeNow()
          }
        })
    }
  }

  @Test
  @DisplayName("When sending an invalid CreateCommand")
  fun a1(tc: VertxTestContext) {
    val invalidCommand = JsonObject()
    client.post("/commands/customer/1/create")
      .`as`(BodyCodec.none())
      .expect(ResponsePredicate.SC_BAD_REQUEST)
      .sendJson(invalidCommand, tc.succeeding { response: HttpResponse<Void>? -> tc.verify { tc.completeNow() } }
      )
  }

  @Test
  @DisplayName("When sending an invalid CreateCommand expecting uow id")
  fun a2(tc: VertxTestContext) {
    val cmdAsJson = JsonObject("{\"type\":\"io.github.crabzilla.example1.CreateCustomer\",\"name\":\"a bad name\"}")
    log.info("/commands/customer/{}/create with json \n {}", customerId2, cmdAsJson.encodePrettily())
    client.post("/commands/customer/$nextInt/create")
      .`as`(BodyCodec.none())
      .expect(ResponsePredicate.SC_BAD_REQUEST)
      .sendJson(cmdAsJson, tc.succeeding { response: HttpResponse<Void>? -> tc.verify { tc.completeNow() } }
      )
  }

  @Test
  @DisplayName("When sending a command with invalid type")
  fun a3(tc: VertxTestContext) {
    val invalidCommand = JsonObject()
    val commandWithoutType = "doSomething"
    client.post("/commands/customer/1/$commandWithoutType")
      .`as`(BodyCodec.jsonObject())
      .expect(ResponsePredicate.SC_BAD_REQUEST)
      .sendJson(invalidCommand, tc.succeeding {
        response: HttpResponse<JsonObject> -> tc.verify {
          assertThat(response.statusMessage()).isEqualTo("Cannot decode the json for command doSomething")
          tc.completeNow()
        }
      }
    )
  }

  @Test
  @DisplayName("When GET to an invalid UnitOfWork (bad number) You get a 400")
  fun a4(tc: VertxTestContext) {
    client["/commands/customer/units-of-work/dddd"]
      .`as`(BodyCodec.jsonObject())
      .expect(ResponsePredicate.SC_BAD_REQUEST)
      .send(tc.succeeding { response: HttpResponse<JsonObject> ->
        tc.verify {
          assertThat(response.statusMessage()).isEqualTo("path param unitOfWorkId must be a number")
          tc.completeNow()
        }
      })
  }
}
