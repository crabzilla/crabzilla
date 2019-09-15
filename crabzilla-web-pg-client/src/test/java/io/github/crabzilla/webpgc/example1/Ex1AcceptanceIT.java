package io.github.crabzilla.webpgc.example1;

import io.github.crabzilla.example1.customer.CreateCustomer;
import io.github.crabzilla.example1.customer.UnknownCommand;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgPool;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.util.Random;
import java.util.UUID;

import static io.github.crabzilla.framework.UnitOfWork.JsonMetadata.*;
import static io.github.crabzilla.pgc.PgcKt.readModelPgPool;
import static io.github.crabzilla.pgc.PgcKt.writeModelPgPool;
import static io.github.crabzilla.webpgc.WebpgcKt.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
  Integration test
**/
@ExtendWith(VertxExtension.class)
class Ex1AcceptanceIT {

  static {
    System.setProperty(io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME,
      SLF4JLogDelegateFactory.class.getName());
    LoggerFactory.getLogger(io.vertx.core.logging.LoggerFactory.class);// Required for Logback to work in Vertx
  }

  private static final Logger log = LoggerFactory.getLogger(Ex1AcceptanceIT.class);

  static final Random random = new Random();
  static int nextInt = random.nextInt();
  static int customerId2 = random.nextInt();

  private static WebClient client;

  private static int readHttpPort = findFreeHttpPort();
  private static int writeHttpPort = findFreeHttpPort();

  private static int findFreeHttpPort() {
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
    getConfig(vertx,  "./../example1.env")
      .setHandler(gotConfig -> {
        if (gotConfig.succeeded()) {
          JsonObject config = gotConfig.result();
          config.put("WRITE_HTTP_PORT", writeHttpPort);
          config.put("READ_HTTP_PORT", readHttpPort);
          DeploymentOptions deploymentOptions = new DeploymentOptions().setConfig(config).setInstances(1);
          WebClientOptions wco = new WebClientOptions();
          client = WebClient.create(vertx, wco);
          final String processId = ManagementFactory.getRuntimeMXBean().getName();
          CompositeFuture.all(
            deploy(vertx, Ex1WebCommandVerticle.class.getName(), deploymentOptions),
            deploy(vertx, Ex1WebQueryVerticle.class.getName(), deploymentOptions),
            deploySingleton(vertx, Ex1DbProjectionsVerticle.class.getName(), deploymentOptions, processId))
            .setHandler(event -> {
              if (event.failed()) {
                tc.failNow(event.cause());
                return;
              }
              PgPool read = readModelPgPool(vertx, config);
              PgPool write = writeModelPgPool(vertx, config);
              write.query("delete from units_of_work", event1 -> {
                if (event1.failed()) {
                  tc.failNow(event1.cause());
                  return;
                }
                write.query("delete from customer_snapshots", event2 -> {
                  if (event2.failed()) {
                    tc.failNow(event2.cause());
                    return;
                  }
                  read.query("delete from customer_summary", event3 -> {
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
        } else {
          tc.failNow(gotConfig.cause());
        }
      }
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
      client.post(writeHttpPort, "0.0.0.0", "/commands/customers/" + customerId2 + "/create"  )
        .as(BodyCodec.jsonObject())
        .expect(ResponsePredicate.SC_SUCCESS)
        .expect(ResponsePredicate.JSON)
        .sendJson(cmdAsJson, tc.succeeding(response1 -> tc.verify(() -> {
            JsonObject uow = response1.body();
            Long uowId = Long.valueOf(response1.getHeader("uowId"));
            assertThat(uowId).isPositive();
            assertThat(uow.getString(ENTITY_NAME)).isEqualTo("customer");
            assertThat(uow.getInteger(ENTITY_ID)).isEqualTo(customerId2);
            assertThat(uow.getString(COMMAND_ID)).isNotNull();
            assertThat(uow.getString(COMMAND_NAME)).isEqualTo("create");
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
      client.get(writeHttpPort, "0.0.0.0", "/commands/customers/" + customerId2)
        .as(BodyCodec.jsonObject())
        .expect(ResponsePredicate.SC_SUCCESS)
        .expect(ResponsePredicate.JSON)
        .send(tc.succeeding(response2 -> tc.verify(() -> {
          JsonObject jo = response2.body();
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
    void a22(VertxTestContext tc) {
      client.get(readHttpPort, "0.0.0.0", "/customers/" + customerId2)
        .as(BodyCodec.jsonObject())
        .expect(ResponsePredicate.SC_SUCCESS)
        .expect(ResponsePredicate.JSON)
        .send(tc.succeeding(response2 -> tc.verify(() -> {
          JsonObject jo = response2.body();
          assertThat(jo.encode())
            .isEqualTo("{\"id\":" + customerId2 + ",\"name\":\"customer#" + customerId2 + "\",\"is_active\":false}");
          tc.completeNow();
        })));
    }

    @Test
    @DisplayName("You get a correspondent entity tracking") // TODO
    void a3(VertxTestContext tc) {
      client.get(writeHttpPort, "0.0.0.0", "/commands/customers/" + customerId2 + "/units-of-work")
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
      CreateCustomer cmd = new CreateCustomer("customer#" + customerId3);
      JsonObject cmdAsJson = JsonObject.mapFrom(cmd);
      UUID cmdId = UUID.randomUUID();
      client.post(writeHttpPort, "0.0.0.0", "/commands/customers/" + customerId3 + "/create")
        .as(BodyCodec.jsonObject())
        .putHeader("commandId", cmdId.toString())
        .expect(ResponsePredicate.SC_SUCCESS)
        .expect(ResponsePredicate.JSON)
        .sendJson(cmdAsJson, tc.succeeding(response1 -> tc.verify(() -> {
            JsonObject uow1 = response1.body();
            Long uowId1 = Long.valueOf(response1.getHeader("uowId"));
            assertThat(uowId1).isPositive();
            assertThat(uow1.getString(ENTITY_NAME)).isEqualTo("customer");
            assertThat(uow1.getInteger(ENTITY_ID)).isEqualTo(customerId3);
            assertThat(uow1.getString(COMMAND_ID)).isNotNull();
            assertThat(uow1.getString(COMMAND_NAME)).isEqualTo("create");
            assertThat(uow1.getJsonObject(COMMAND)).isEqualTo(cmdAsJson);
            assertThat(uow1.getInteger(VERSION)).isEqualTo(1);
            assertThat(uow1.getJsonArray(EVENTS).size()).isEqualTo(1);
            client.post(writeHttpPort, "0.0.0.0", "/commands/customers/" + customerId3 + "/create")
              .as(BodyCodec.jsonObject())
              .putHeader("commandId", cmdId.toString())
              .expect(ResponsePredicate.SC_SUCCESS)
              .expect(ResponsePredicate.JSON)
              .sendJson(cmdAsJson, tc.succeeding(response2 -> tc.verify(() -> {
                JsonObject uow2 = response2.body();
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
      client.get(writeHttpPort, "0.0.0.0", "/commands/customers/" + customerId3)
        .as(BodyCodec.jsonObject())
        .expect(ResponsePredicate.SC_SUCCESS)
        .expect(ResponsePredicate.JSON)
        .send(tc.succeeding(response2 -> tc.verify(() -> {
          JsonObject jo = response2.body();
          System.out.println(jo.encodePrettily());
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
    void a22(VertxTestContext tc) {
      client.get(readHttpPort, "0.0.0.0", "/customers/" + customerId3)
        .as(BodyCodec.jsonObject())
        .expect(ResponsePredicate.SC_SUCCESS)
        .expect(ResponsePredicate.JSON)
        .send(tc.succeeding(response2 -> tc.verify(() -> {
          JsonObject jo = response2.body();
          assertThat(jo.encode())
            .isEqualTo("{\"id\":" + customerId3 + ",\"name\":\"customer#" + customerId3 + "\",\"is_active\":false}");
          tc.completeNow();
        })));
    }

    @Test
    @DisplayName("You get a correspondent entity tracking") // TODO
    void a3(VertxTestContext tc) {
      client.get(writeHttpPort, "0.0.0.0", "/commands/customers/" + customerId3 + "/units-of-work")
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
    client.post(writeHttpPort, "0.0.0.0", "/commands/customers/1/create")
      .as(BodyCodec.none())
      .expect(ResponsePredicate.SC_BAD_REQUEST)
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
    client.post(writeHttpPort, "0.0.0.0", "/commands/customers/" + nextInt + "/create")
      .as(BodyCodec.none())
      .expect(ResponsePredicate.SC_BAD_REQUEST)
      .sendJson(jo, tc.succeeding(response -> tc.verify(() -> {
          tc.completeNow();
        }))
      );
  }

  @Test
  @DisplayName("When sending an UnknownCommand")
  void a5(VertxTestContext tc) {
    UnknownCommand cmd = new UnknownCommand(nextInt);
    JsonObject jo = JsonObject.mapFrom(cmd);
    client.post(writeHttpPort, "0.0.0.0", "/commands/customers/" + nextInt + "/unknown")
      .as(BodyCodec.none())
      .expect(ResponsePredicate.SC_BAD_REQUEST)
      .sendJson(jo, tc.succeeding(response -> tc.verify(() -> {
          tc.completeNow();
        }))
      );
  }


    @Test
    @DisplayName("When GET to an invalid UnitOfWork (bad number) You get a 400")
    void a1(VertxTestContext tc) {
      client.get(writeHttpPort, "0.0.0.0", "/commands/customers/units-of-work/dddd")
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
