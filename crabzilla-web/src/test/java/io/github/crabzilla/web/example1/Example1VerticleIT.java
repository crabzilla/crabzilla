package io.github.crabzilla.web.example1;

import io.github.crabzilla.example1.CreateCustomer;
import io.github.crabzilla.example1.CustomerId;
import io.github.crabzilla.example1.UnknownCommand;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ServerSocket;
import java.util.Random;

import static io.github.crabzilla.UnitOfWork.JsonMetadata.*;
import static io.github.crabzilla.example1.CustomerCommandEnum.CREATE;
import static io.github.crabzilla.web.ContentTypes.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
  Integration test
**/
@ExtendWith(VertxExtension.class)
class Example1VerticleIT {

  static {
    System.setProperty(io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME,
      SLF4JLogDelegateFactory.class.getName());
    LoggerFactory.getLogger(io.vertx.core.logging.LoggerFactory.class);// Required for Logback to work in Vertx
  }

  static Example1Verticle verticle;
  static WebClient client;
  static int port;

  static final Random random = new Random();
  static int nextInt = random.nextInt();
  static int customerId2 = random.nextInt();
  static final Logger log = LoggerFactory.getLogger(Example1VerticleIT.class);

  private static int httpPort() {
    int httpPort = 0;
    try {
      ServerSocket socket = new ServerSocket(0);
      httpPort = socket.getLocalPort();
      socket.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return httpPort;
  }

  @BeforeAll
  static void setup(VertxTestContext tc, Vertx vertx) {
    port = httpPort();
    verticle = new Example1Verticle(port, "../example1.env");
    log.info("will try to deploy MainVerticle using HTTP_PORT = " + port);
    WebClientOptions wco = new WebClientOptions();
    client = WebClient.create(vertx, wco);

    vertx.deployVerticle(verticle, deploy -> {
      if (deploy.succeeded()) {
        verticle.writeModelDb.query("delete from units_of_work", event1 -> {
          if (event1.failed()) {
            tc.failNow(event1.cause());
            return;
          }
          verticle.writeModelDb.query("delete from customer_snapshots", event2 -> {
            if (event2.failed()) {
              tc.failNow(event2.cause());
              return;
            }
            verticle.readModelDb.query("delete from customer_summary", event3 -> {
              if (event3.failed()) {
                tc.failNow(event3.cause());
                return;
              }
              tc.completeNow();
            });
          });
        });
      } else {
        deploy.cause().printStackTrace();
        tc.failNow(deploy.cause());
      }
    });
  }

  @Test
  @DisplayName("When sending a valid CreateCommand expecting uow id")
  void a1(VertxTestContext tc) {
    CreateCustomer cmd = new CreateCustomer("customer#" + nextInt);
    JsonObject jo = JsonObject.mapFrom(cmd);
    client.post(port, "0.0.0.0", "/customers/" + nextInt + "/commands/" + CREATE.urlFriendly())
      .as(BodyCodec.jsonObject())
      .expect(ResponsePredicate.SC_SUCCESS)
      .expect(ResponsePredicate.JSON)
      .putHeader("accept", UNIT_OF_WORK_ID)
      .sendJsonObject(jo, tc.succeeding(response -> tc.verify(() -> {
          assertThat(response.body().getString("unitOfWorkId")).isNotNull();
          tc.completeNow();
      }))
    );
  }

  @Nested
  @DisplayName("When sending a valid CreateCommand expecting uow body")
  class When1 {

    @Test
    @DisplayName("You get a correspondent UnitOfWork as JSON")
    void a1(VertxTestContext tc) {
      CreateCustomer cmd = new CreateCustomer("customer#" + customerId2);
      JsonObject cmdAsJson = JsonObject.mapFrom(cmd);
      client.post(port, "0.0.0.0", "/customers/" + customerId2 + "/commands/" + CREATE.urlFriendly())
        .as(BodyCodec.jsonObject())
        .expect(ResponsePredicate.SC_SUCCESS)
        .expect(ResponsePredicate.JSON)
        .putHeader("accept", UNIT_OF_WORK_BODY)
        .sendJson(cmdAsJson, tc.succeeding(response1 -> tc.verify(() -> {
            JsonObject uow = response1.body();
            System.out.println(uow.encodePrettily());
            final String uowIdStr = uow.getString(UOW_ID);
            assertThat(uow.getString(UOW_ID)).isNotNull();
            assertThat(uow.getString(ENTITY_NAME)).isEqualTo("customer");
            assertThat(uow.getInteger(ENTITY_ID)).isEqualTo(customerId2);
            assertThat(uow.getString(COMMAND_ID)).isNotNull();
            assertThat(uow.getString(COMMAND_NAME)).isEqualTo("create");
            assertThat(uow.getJsonObject(COMMAND)).isEqualTo(cmdAsJson);
            assertThat(uow.getInteger(VERSION)).isEqualTo(1);
            assertThat(uow.getJsonArray(EVENTS).size()).isEqualTo(1);
            client.get(port, "0.0.0.0", "/units-of-work/" + uowIdStr)
              .as(BodyCodec.jsonObject())
              .expect(ResponsePredicate.SC_SUCCESS)
              .expect(ResponsePredicate.JSON)
              .send(tc.succeeding(response3 -> tc.verify(() -> {
                JsonObject jo2 = response3.body();
                assertThat(jo2.getString(UOW_ID)).isNotNull();
                assertThat(jo2.getString(ENTITY_NAME)).isEqualTo("customer");
                assertThat(jo2.getInteger(ENTITY_ID)).isEqualTo(customerId2);
                assertThat(jo2.getString(COMMAND_ID)).isNotNull();
                assertThat(jo2.getString(COMMAND_NAME)).isEqualTo("create");
                assertThat(jo2.getJsonObject(COMMAND)).isEqualTo(cmdAsJson);
                assertThat(jo2.getInteger(VERSION)).isEqualTo(1);
                assertThat(jo2.getJsonArray(EVENTS).size()).isEqualTo(1);
                System.out.println(jo2);
                tc.completeNow();
              })));
        }))
      );
    }

    @Test
    @DisplayName("You get a correspondent snapshot (write model)")
    void a2(VertxTestContext tc) {
      client.get(port, "0.0.0.0", "/customers/" + customerId2)
        .putHeader("accept", ENTITY_WRITE_MODEL)
        .as(BodyCodec.jsonObject())
        .expect(ResponsePredicate.SC_SUCCESS)
        .expect(ResponsePredicate.JSON)
        .send(tc.succeeding(response2 -> tc.verify(() -> {
          JsonObject jo = response2.body();
          System.out.println(jo.encodePrettily());
          assertThat(jo.getInteger("version")).isEqualTo(1);
          JsonObject state = jo.getJsonObject("state");
          assertThat(state.getInteger("customerId")).isEqualTo(customerId2);
          assertThat(state.getString("name")).isEqualTo("customer#" + customerId2);
          assertThat(state.getBoolean("isActive")).isFalse();
          tc.completeNow();
        })));
    }

    @Test
    @DisplayName("You get a correspondent entity tracking")
    void a3(VertxTestContext tc) {
      client.get(port, "0.0.0.0", "/customers/" + customerId2)
        .putHeader("accept", ENTITY_TRACKING)
        .as(BodyCodec.jsonObject())
        .expect(ResponsePredicate.SC_SUCCESS)
        .expect(ResponsePredicate.JSON)
        .send(tc.succeeding(response2 -> tc.verify(() -> {
          JsonObject jo = response2.body();
          System.out.println(jo.encodePrettily());
          JsonObject snapshot = jo.getJsonObject("snapshot");
          assertThat(snapshot.getInteger("version")).isEqualTo(1);
          JsonObject state = snapshot.getJsonObject("state");
          assertThat(state.getInteger("customerId")).isEqualTo(customerId2);
          assertThat(state.getString("name")).isEqualTo("customer#" + customerId2);
          assertThat(state.getBoolean("isActive")).isFalse();
          assertThat(jo.getJsonArray("units_of_work").size()).isEqualTo(1);
          tc.completeNow();
        })));
    }

  }

  @Test
  @DisplayName("When sending an invalid CreateCommand")
  void a3(VertxTestContext tc) {
    JsonObject invalidCommand = new JsonObject();
    client.post(port, "0.0.0.0", "/customers/1/commands/" + CREATE.urlFriendly())
      .as(BodyCodec.none())
      .expect(ResponsePredicate.SC_BAD_REQUEST)
      .putHeader("accept", UNIT_OF_WORK_ID)
      .sendJson(invalidCommand, tc.succeeding(response -> tc.verify(() -> {
          tc.completeNow();
        }))
      );
  }

  @Test
  @DisplayName("When sending an invalid CreateCommand expecting uow id")
  void a4(VertxTestContext tc) {
    CreateCustomer cmd = new CreateCustomer("a bad name");
    JsonObject jo = JsonObject.mapFrom(cmd);
    client.post(port, "0.0.0.0", "/customers/" + nextInt + "/commands/" + CREATE.urlFriendly())
      .as(BodyCodec.none())
      .expect(ResponsePredicate.SC_BAD_REQUEST)
      .putHeader("accept", UNIT_OF_WORK_ID)
      .sendJson(jo, tc.succeeding(response -> tc.verify(() -> {
          tc.completeNow();
        }))
      );
  }

  @Test
  @DisplayName("When sending an UnknownCommand")
  void a5(VertxTestContext tc) {
    UnknownCommand cmd = new UnknownCommand(new CustomerId(nextInt));
    JsonObject jo = JsonObject.mapFrom(cmd);
    client.post(port, "0.0.0.0", "/customers/" + nextInt + "/commands/unknown")
      .as(BodyCodec.none())
      .expect(ResponsePredicate.SC_BAD_REQUEST)
      .putHeader("accept", UNIT_OF_WORK_ID)
      .sendJson(jo, tc.succeeding(response -> tc.verify(() -> {
          tc.completeNow();
        }))
      );
  }

}
