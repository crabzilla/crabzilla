package io.github.crabzilla.webpgc;

import io.github.crabzilla.webpgc.example1.CustomerVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgPool;
import org.assertj.core.api.StringAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.UUID;

import static io.github.crabzilla.core.UnitOfWork.JsonMetadata.*;
import static io.github.crabzilla.webpgc.Pgc_boilerplateKt.readModelPgPool;
import static io.github.crabzilla.webpgc.Pgc_boilerplateKt.writeModelPgPool;
import static io.github.crabzilla.webpgc.Web_pgc_boilerplateKt.*;
import static io.vertx.junit5.web.TestRequest.statusCode;
import static io.vertx.junit5.web.TestRequest.testRequest;
import static org.assertj.core.api.Assertions.assertThat;

/**
  Integration test
**/
@ExtendWith(VertxExtension.class)
class CustomerAcceptanceIT {

  static {
    System.setProperty(io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME,
      SLF4JLogDelegateFactory.class.getName());
    LoggerFactory.getLogger(io.vertx.core.logging.LoggerFactory.class);// Required for Logback to work in Vertx
  }

  private static final Logger log = LoggerFactory.getLogger(CustomerAcceptanceIT.class);

  static final Random random = new Random();
  static int nextInt = random.nextInt();
  static int customerId2 = random.nextInt();

  private static WebClient client;

  private static int httpPort = findFreeHttpPort();

  @BeforeAll
  static void setup(VertxTestContext tc, Vertx vertx) {
    getConfig(vertx,  "./../example1.env")
      .onSuccess(config -> {
        config.put("HTTP_PORT", httpPort);
        DeploymentOptions deploymentOptions = new DeploymentOptions().setConfig(config).setInstances(1);
        WebClientOptions wco = new WebClientOptions();
        wco.setDefaultHost("0.0.0.0");
        wco.setDefaultPort(httpPort);
        client = WebClient.create(vertx, wco);
        deploy(vertx, CustomerVerticle.class.getName(), deploymentOptions)
          .onComplete(event -> {
            PgPool read = readModelPgPool(vertx, config);
            PgPool write = writeModelPgPool(vertx, config);
            write.query("delete from units_of_work").execute(event1 -> {
              if (event1.failed()) {
                tc.failNow(event1.cause());
                return;
              }
              write.query("delete from customer_snapshots").execute( event2 -> {
                if (event2.failed()) {
                  tc.failNow(event2.cause());
                  return;
                }
                read.query("delete from customer_summary").execute( event3 -> {
                  if (event3.failed()) {
                    tc.failNow(event3.cause());
                    return;
                  }
                  tc.completeNow();
                });
              });
            });
          }
        );
      }).onFailure(tc::failNow);
  }

  @Nested
  @DisplayName("When sending a valid CreateCommand expecting uow body")
  class When1 {

    @Test
    @DisplayName("You get a correspondent UnitOfWork as JSON")
    void a1(VertxTestContext tc) {
      JsonObject cmdAsJson = new JsonObject("{\"name\":\"customer#" + customerId2 + "\"}");
      log.info("/commands/customers/{}/create with json \n {}", customerId2, cmdAsJson.encodePrettily());
      client.post(httpPort, "0.0.0.0", "/commands/customers/" + customerId2 + "/create"  )
        .as(BodyCodec.jsonObject())
        .expect(ResponsePredicate.SC_SUCCESS)
        .expect(ResponsePredicate.JSON)
        .sendJson(cmdAsJson, tc.succeeding(response1 -> tc.verify(() -> {
            JsonObject uow = response1.body();
            log.info("UnitOfWork {}", uow.encodePrettily());
            Long uowId = Long.valueOf(response1.getHeader("uowId"));
            assertThat(uowId).isPositive();
            assertThat(uow.getString(ENTITY_NAME)).isEqualTo("customer");
            assertThat(uow.getInteger(ENTITY_ID)).isEqualTo(customerId2);
            assertThat(uow.getString(COMMAND_ID)).isNotNull();
            assertThat(uow.getJsonObject(COMMAND)).isEqualTo(cmdAsJson);
            assertThat(uow.getInteger(VERSION)).isEqualTo(1);
            assertThat(uow.getJsonArray(EVENTS).size()).isEqualTo(1);
            tc.completeNow();
        }))
      );
    }

    @Test
    @DisplayName("You get a correspondent snapshot (write model)")
    void a2(VertxTestContext tc) {
      client.get(httpPort, "0.0.0.0", "/commands/customers/" + customerId2)
        .as(BodyCodec.jsonObject())
        .expect(ResponsePredicate.SC_SUCCESS)
        .expect(ResponsePredicate.JSON)
        .send(tc.succeeding(response2 -> tc.verify(() -> {
          JsonObject jo = response2.body();
          log.info("UnitOfWork {}", jo.encodePrettily());
          assertThat(jo.getInteger("version")).isEqualTo(1);
          JsonObject state = jo.getJsonObject("state");
          assertThat(state.getInteger("customerId")).isEqualTo(customerId2);
          assertThat(state.getString("name")).isEqualTo("customer#" + customerId2);
          assertThat(state.getBoolean("isActive")).isFalse();
          tc.completeNow();
        })));
    }

    @Test
    @DisplayName("You get a correspondent company summary (read model)")
    void a3(VertxTestContext tc) {
      String route = "/customers/" + customerId2;
      JsonObject expected =
        new JsonObject("{\"id\":" + customerId2 + ",\"name\":\"customer#" + customerId2 + "\",\"is_active\":false}");
      testRequest(client, HttpMethod.GET, route)
        .expect(statusCode(200))
        .expect(HttpResponse::bodyAsJsonObject)
        .expect(response ->
          System.out.println(response.bodyAsString()))
        .expect(response ->
          new StringAssert(response.bodyAsJsonObject().encode()).isEqualToIgnoringWhitespace(expected.encode()))
        .send(tc);
    }

    @Test
    @DisplayName("You get a correspondent entity tracking")
    void a4(VertxTestContext tc) {
      client.get(httpPort, "0.0.0.0", "/commands/customers/" + customerId2 + "/units-of-work")
        .as(BodyCodec.jsonArray())
        .expect(ResponsePredicate.SC_SUCCESS)
        .expect(ResponsePredicate.JSON)
        .send(tc.succeeding(response2 -> tc.verify(() -> {
          JsonArray array = response2.body();
          assertThat(array.size()).isEqualTo(1);
          tc.completeNow();
        })));
    }

  }

  @Nested
  @DisplayName("When sending the same idempotent valid CreateCommand 2 times expecting uow body")
  class When2 {

    int customerId3 = customerId2 + 1;
    @Test
    @DisplayName("You get a correspondent UnitOfWork as JSON")
    void a1(VertxTestContext tc) {
      JsonObject cmdAsJson = new JsonObject("{\"name\":\"customer#" + customerId3 + "\"}");
      log.info("/commands/customers/{}/create with json \n {}", customerId3, cmdAsJson.encodePrettily());
      UUID cmdId = UUID.randomUUID();
      client.post(httpPort, "0.0.0.0", "/commands/customers/" + customerId3 + "/create")
        .as(BodyCodec.jsonObject())
        .putHeader("commandId", cmdId.toString())
        .expect(ResponsePredicate.SC_SUCCESS)
        .expect(ResponsePredicate.JSON)
        .sendJson(cmdAsJson, tc.succeeding(response1 -> tc.verify(() -> {
          JsonObject uow1 = response1.body();
          log.info("UnitOfWork {}", uow1.encodePrettily());
          Long uowId1 = Long.valueOf(response1.getHeader("uowId"));
            assertThat(uowId1).isPositive();
            assertThat(uow1.getString(ENTITY_NAME)).isEqualTo("customer");
            assertThat(uow1.getInteger(ENTITY_ID)).isEqualTo(customerId3);
            assertThat(uow1.getString(COMMAND_ID)).isNotNull();
            assertThat(uow1.getJsonObject(COMMAND)).isEqualTo(cmdAsJson);
            assertThat(uow1.getInteger(VERSION)).isEqualTo(1);
            assertThat(uow1.getJsonArray(EVENTS).size()).isEqualTo(1);
            client.post(httpPort, "0.0.0.0", "/commands/customers/" + customerId3 + "/create")
              .as(BodyCodec.jsonObject())
              .putHeader("commandId", cmdId.toString())
              .expect(ResponsePredicate.SC_SUCCESS)
              .expect(ResponsePredicate.JSON)
              .sendJson(cmdAsJson, tc.succeeding(response2 -> tc.verify(() -> {
                JsonObject uow2 = response2.body();
                System.out.println(uow2.encodePrettily());
                Long uowId2 = Long.valueOf(response2.getHeader("uowId"));
                assertThat(uow2).isEqualTo(uow1);
                assertThat(uowId2).isEqualTo(uowId1);
                tc.completeNow();
              }))
            );
          }))
        );
    }

    @Test
    @DisplayName("You get a correspondent snapshot (write model)")
    void a2(VertxTestContext tc) {
      client.get(httpPort, "0.0.0.0", "/commands/customers/" + customerId3)
        .as(BodyCodec.jsonObject())
        .expect(ResponsePredicate.SC_SUCCESS)
        .expect(ResponsePredicate.JSON)
        .send(tc.succeeding(response2 -> tc.verify(() -> {
          JsonObject jo = response2.body();
          assertThat(jo.getInteger("version")).isEqualTo(1);
          JsonObject state = jo.getJsonObject("state");
          assertThat(state.getInteger("customerId")).isEqualTo(customerId3);
          assertThat(state.getString("name")).isEqualTo("customer#" + customerId3);
          assertThat(state.getBoolean("isActive")).isFalse();
          tc.completeNow();
        })));
    }

    @Test
    @DisplayName("You get a correspondent company summary (read model)")
    void a3(VertxTestContext tc) {
      String route = "/customers/" + customerId3;
      String expected = "{\"id\":" + customerId3 + ",\"name\":\"customer#" + customerId3 + "\",\"is_active\":false}";
      testRequest(client, HttpMethod.GET, route)
        .expect(statusCode(200))
        .expect(HttpResponse::bodyAsJsonObject)
        .expect(bufferHttpResponse -> bufferHttpResponse.bodyAsString().equals(expected))
        .send(tc);
    }

    @Test
    @DisplayName("You get a correspondent entity tracking")
    void a4(VertxTestContext tc) {
      client.get(httpPort, "0.0.0.0", "/commands/customers/" + customerId3 + "/units-of-work")
        .as(BodyCodec.jsonArray())
        .expect(ResponsePredicate.SC_SUCCESS)
        .expect(ResponsePredicate.JSON)
        .send(tc.succeeding(response2 -> tc.verify(() -> {
          JsonArray array = response2.body();
          assertThat(array.size()).isEqualTo(1);
          tc.completeNow();
        })));
    }

  }

  @Test
  @DisplayName("When sending an invalid CreateCommand")
  void a3(VertxTestContext tc) {
    JsonObject invalidCommand = new JsonObject();
    client.post(httpPort, "0.0.0.0", "/commands/customers/1/create")
      .as(BodyCodec.none())
      .expect(ResponsePredicate.SC_BAD_REQUEST)
      .sendJson(invalidCommand, tc.succeeding(response -> tc.verify(tc::completeNow))
      );
  }

  @Test
  @DisplayName("When sending an invalid CreateCommand expecting uow id")
  void a4(VertxTestContext tc) {
    JsonObject cmdAsJson =
      new JsonObject("{\"type\":\"io.github.crabzilla.example1.CreateCustomer\",\"name\":\"a bad name\"}");
    log.info("/commands/customers/{}/create with json \n {}", customerId2, cmdAsJson.encodePrettily());
    client.post(httpPort, "0.0.0.0", "/commands/customers/" + nextInt + "/create")
      .as(BodyCodec.none())
      .expect(ResponsePredicate.SC_BAD_REQUEST)
      .sendJson(cmdAsJson, tc.succeeding(response -> tc.verify(tc::completeNow))
      );
  }

    @Test
    @DisplayName("When GET to an invalid UnitOfWork (bad number) You get a 400")
    void a1(VertxTestContext tc) {
      client.get(httpPort, "0.0.0.0", "/commands/customers/units-of-work/dddd")
        .as(BodyCodec.string())
        .expect(ResponsePredicate.SC_BAD_REQUEST)
        .send(tc.succeeding(response -> tc.verify(() -> {
            String result = response.body();
            assertThat(result).isEqualTo("path param unitOfWorkId must be a number");
            tc.completeNow();
          }))
        );
    }

}
