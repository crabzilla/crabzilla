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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ServerSocket;
import java.util.Random;

import static io.github.crabzilla.example1.CustomerCommandEnum.CREATE;
import static io.github.crabzilla.web.WebKt.CONTENT_TYPE_UNIT_OF_WORK_BODY;
import static io.github.crabzilla.web.WebKt.CONTENT_TYPE_UNIT_OF_WORK_ID;
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
    verticle = new Example1Verticle(port);
    log.info("will try to deploy MainVerticle using HTTP_PORT = " + port);
    WebClientOptions wco = new WebClientOptions();
    client = WebClient.create(vertx, wco);
    vertx.deployVerticle(verticle, deploy -> {
      if (deploy.succeeded()) {
        tc.completeNow();
      } else {
        deploy.cause().printStackTrace();
        tc.failNow(deploy.cause());
      }
    });
  }

  @Test
  @DisplayName("When sending a valid CreateCommand expecting uow id")
  void a1(VertxTestContext tc) {
    int nextInt = random.nextInt();
    CreateCustomer cmd = new CreateCustomer("customer#" + nextInt);
    JsonObject jo = JsonObject.mapFrom(cmd);
    client.post(port, "0.0.0.0", "/customers/" + nextInt + "/commands/" + CREATE.urlFriendly())
      .as(BodyCodec.jsonObject())
      .expect(ResponsePredicate.SC_SUCCESS)
      .expect(ResponsePredicate.JSON)
      .putHeader("accept", CONTENT_TYPE_UNIT_OF_WORK_ID)
      .sendJsonObject(jo, tc.succeeding(response -> tc.verify(() -> {
          assertThat(response.body().getString("unitOfWorkId")).isNotNull();
          tc.completeNow();
      }))
    );
  }

  @Test
  @DisplayName("When sending a valid CreateCommand expecting uow body")
  void a2(VertxTestContext tc) {
    int nextInt = random.nextInt();
    CreateCustomer cmd = new CreateCustomer("customer#" + nextInt);
    JsonObject jo = JsonObject.mapFrom(cmd);
    client.post(port, "0.0.0.0", "/customers/" + nextInt + "/commands/" + CREATE.urlFriendly())
      .as(BodyCodec.jsonObject())
      .expect(ResponsePredicate.SC_SUCCESS)
      .expect(ResponsePredicate.JSON)
      .putHeader("accept", CONTENT_TYPE_UNIT_OF_WORK_BODY)
      .sendJson(jo, tc.succeeding(response -> tc.verify(() -> {
          assertThat(response.body().getString("unitOfWorkId")).isNotNull();
          assertThat(response.body().getJsonObject("command")).isEqualTo(jo);
          // TODO
          tc.completeNow();
      }))
    );
  }

  @Test
  @DisplayName("When sending an invalid CreateCommand")
  void a3(VertxTestContext tc) {
    JsonObject invalidCommand = new JsonObject();
    client.post(port, "0.0.0.0", "/customers/1/commands/" + CREATE.urlFriendly())
      .as(BodyCodec.none())
      .expect(ResponsePredicate.SC_BAD_REQUEST)
      .putHeader("accept", CONTENT_TYPE_UNIT_OF_WORK_ID)
      .sendJson(invalidCommand, tc.succeeding(response -> tc.verify(() -> {
          tc.completeNow();
        }))
      );
  }

  @Test
  @DisplayName("When sending an invalid CreateCommand expecting uow id")
  void a4(VertxTestContext tc) {
    int nextInt = random.nextInt();
    CreateCustomer cmd = new CreateCustomer("a bad name");
    JsonObject jo = JsonObject.mapFrom(cmd);
    client.post(port, "0.0.0.0", "/customers/" + nextInt + "/commands/" + CREATE.urlFriendly())
      .as(BodyCodec.none())
      .expect(ResponsePredicate.SC_BAD_REQUEST)
      .putHeader("accept", CONTENT_TYPE_UNIT_OF_WORK_ID)
      .sendJson(jo, tc.succeeding(response -> tc.verify(() -> {
          tc.completeNow();
        }))
      );
  }

  @Test
  @DisplayName("When sending an UnknownCommand")
  void a5(VertxTestContext tc) {
    int nextInt = random.nextInt();
    UnknownCommand cmd = new UnknownCommand(new CustomerId(nextInt));
    JsonObject jo = JsonObject.mapFrom(cmd);
    client.post(port, "0.0.0.0", "/customers/" + nextInt + "/commands/unknown")
      .as(BodyCodec.none())
      .expect(ResponsePredicate.SC_BAD_REQUEST)
      .putHeader("accept", CONTENT_TYPE_UNIT_OF_WORK_ID)
      .sendJson(jo, tc.succeeding(response -> tc.verify(() -> {
          tc.completeNow();
        }))
      );
  }

}
