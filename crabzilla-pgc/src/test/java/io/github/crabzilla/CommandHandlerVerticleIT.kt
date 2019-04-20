package io.github.crabzilla

import io.github.crabzilla.example1.*
import io.github.crabzilla.example1.CustomerCommandEnum.CREATE
import io.github.crabzilla.pgc.PgcSnapshotRepo
import io.github.crabzilla.pgc.PgcUowJournal
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgPool
import io.reactiverse.pgclient.PgPoolOptions
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.ReplyException
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.util.*


@ExtendWith(VertxExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CommandHandlerVerticleIT {

  // https://slinkydeveloper.com/Assertions-With-Vertx-Futures-And-JUnit5/
  private lateinit var vertx: Vertx
  private lateinit var writeDb: PgPool
  private lateinit var verticle: CommandHandlerVerticle<Customer>

  private val options = DeliveryOptions().setCodecName("Command")

  companion object {
    const val aggregateName = "Customer"
    val customerId = CustomerId(1)
    val createCmd = CreateCustomer(customerId, "customer")
    val created = CustomerCreated(customerId, "customer")
    val activateCmd = ActivateCustomer(customerId, "I want it")
    val activated = CustomerActivated("a good reason", Instant.now())
  }

  @BeforeEach
  fun setup(tc: VertxTestContext) {

    val vertxOptions = VertxOptions()
    vertxOptions.blockedThreadCheckInterval = (1000 * 60 * 60).toLong() // to easier debug

    vertx = Vertx.vertx(vertxOptions)

    initVertx(vertx)

    val envOptions = ConfigStoreOptions()
      .setType("file")
      .setFormat("properties")
      .setConfig(JsonObject().put("path", "../example1.env"))

    val retriever = ConfigRetriever.create(vertx, ConfigRetrieverOptions().addStore(envOptions))

    retriever.getConfig(Handler { configFuture ->

      if (configFuture.failed()) {
        println("Failed to get configuration")
        tc.failNow(configFuture.cause())
        return@Handler
      }

      val config = configFuture.result()

      // println(config.encodePrettily())

      val options = PgPoolOptions()
        .setPort(5432)
        .setHost(config.getString("WRITE_DATABASE_HOST"))
        .setDatabase(config.getString("WRITE_DATABASE_NAME"))
        .setUser(config.getString("WRITE_DATABASE_USER"))
        .setPassword(config.getString("WRITE_DATABASE_PASSWORD"))
        .setMaxSize(config.getInteger("WRITE_DATABASE_POOL_MAX_SIZE"))

      writeDb = PgClient.pool(vertx, options)

      val journal = PgcUowJournal(writeDb, CUSTOMER_CMD_TO_JSON, CUSTOMER_EVENT_TO_JSON)

      val snapshotRepo = PgcSnapshotRepo(writeDb, CUSTOMER_SEED_VALUE, CUSTOMER_STATE_BUILDER,
        CUSTOMER_FROM_JSON, CUSTOMER_EVENT_FROM_JSON)

      verticle = CommandHandlerVerticle(aggregateName, CUSTOMER_CMD_FROM_JSON, CUSTOMER_SEED_VALUE,
                        CUSTOMER_CMD_HANDLER_FACTORY, CUSTOMER_CMD_VALIDATOR, journal, snapshotRepo)

      writeDb.query("delete from units_of_work") { deleteResult ->
        if (deleteResult.failed()) {
          deleteResult.cause().printStackTrace()
          tc.failNow(deleteResult.cause())
          return@query
        }
        vertx.deployVerticle(verticle, Handler { event ->
          if (event.failed()) {
            tc.failNow(event.cause())
          }
          tc.completeNow()
        })
      }

    })

  }

  @Test
  @DisplayName("given a valid command it will be SUCCESS")
  fun a1(tc: VertxTestContext) {

    val customerId = CustomerId(1)
    val createCustomerCmd = CreateCustomer(customerId, "customer1")

    val jo = JsonObject()
    jo.put(JsonMetadata.COMMAND_TARGET_ID, customerId.value)
    jo.put(JsonMetadata.COMMAND_ID, UUID.randomUUID().toString())
    jo.put(JsonMetadata.COMMAND_NAME, CREATE.asPathParam())
    jo.put(JsonMetadata.COMMAND_JSON_CONTENT, JsonObject(CUSTOMER_CMD_TO_JSON(createCustomerCmd)))

    vertx.eventBus()
      .send<Pair<UnitOfWork, Int>>(cmdHandlerEndpoint(aggregateName), jo, options) { asyncResult ->

      if (asyncResult.failed()) {
        tc.failNow(asyncResult.cause())
        return@send
      }

      val result = asyncResult.result().body() as Pair<UnitOfWork, Int>

//      println(result)

      tc.completeNow()

    }

  }

  @Test
  @DisplayName("given an invalid command it will be VALIDATION_ERROR")
  fun a2(tc: VertxTestContext) {

    val customerId = CustomerId(1)
    val createCustomerCmd = CreateCustomer(customerId, "a bad name")

    val jo = JsonObject()
    jo.put(JsonMetadata.COMMAND_TARGET_ID, customerId.value)
    jo.put(JsonMetadata.COMMAND_ID, UUID.randomUUID().toString())
    jo.put(JsonMetadata.COMMAND_NAME, CREATE.asPathParam())
    jo.put(JsonMetadata.COMMAND_JSON_CONTENT, JsonObject(CUSTOMER_CMD_TO_JSON(createCustomerCmd)))

    vertx.eventBus()
      .send<Pair<UnitOfWork, Int>>(cmdHandlerEndpoint(aggregateName), jo, options) { asyncResult ->

      assertThat(asyncResult.succeeded()).isFalse()

      val cause = asyncResult.cause() as ReplyException
      assertThat(cause.message).isEqualTo("[Invalid name: a bad name]")
      assertThat(cause.failureCode()).isEqualTo(400)

      tc.completeNow()

    }

  }

  @Test
  @DisplayName("given an execution error it will be HANDLING_ERROR")
  fun a3(tc: VertxTestContext) {

    val customerId = CustomerId(1)

    val jo = JsonObject()
    jo.put(JsonMetadata.COMMAND_TARGET_ID, customerId.value)
    jo.put(JsonMetadata.COMMAND_ID, UUID.randomUUID().toString())
    jo.put(JsonMetadata.COMMAND_NAME, "unknown")
    jo.put(JsonMetadata.COMMAND_JSON_CONTENT, JsonObject())

    vertx.eventBus()
      .send<Pair<UnitOfWork, Int>>(cmdHandlerEndpoint(aggregateName), jo, options) { asyncResult ->
        tc.verify {
          assertThat(asyncResult.succeeded()).isFalse()
          val cause = asyncResult.cause() as ReplyException
          assertThat(cause.message).isEqualTo("Command cannot be deserialized")
          assertThat(cause.failureCode()).isEqualTo(400)
          tc.completeNow()
        }
    }

  }

}