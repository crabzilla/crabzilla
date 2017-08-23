package io.github.crabzilla.example1

import com.google.inject.Guice
import io.github.crabzilla.example1.customer.CreateCustomer
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCreated
import io.github.crabzilla.example1.customer.CustomerId
import io.github.crabzilla.model.DomainEvent
import io.github.crabzilla.model.EntityUnitOfWork
import io.github.crabzilla.model.Version
import io.github.crabzilla.stack.CommandExecution
import io.github.crabzilla.stack.StringHelper.aggregateRootId
import io.github.crabzilla.vertx.verticles.EntityCommandHandlerVerticle
import io.github.crabzilla.vertx.verticles.EntityCommandRestVerticle
import io.github.crabzilla.vertx.verticles.EventsProjectionVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME
import io.vertx.core.logging.SLF4JLogDelegateFactory
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import lombok.extern.slf4j.Slf4j
import org.jdbi.v3.core.Jdbi
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.lang.System.setProperty
import java.util.*
import javax.inject.Inject

@RunWith(VertxUnitRunner::class)
@Slf4j
class Example1AcceptanceIT {

  private val port = 8080

  private lateinit var vertx: Vertx

  @Inject
  internal lateinit var projectionVerticle: EventsProjectionVerticle<CustomerSummaryDao>

  @Inject
  internal lateinit var restVerticle: EntityCommandRestVerticle<Customer>

  @Inject
  internal lateinit var cmdVerticle: EntityCommandHandlerVerticle<Customer>

  @Inject
  internal var jdbi: Jdbi? = null

  @Before
  @Throws(IOException::class)
  fun setUp(context: TestContext) {

    vertx = Vertx.vertx()

    // Let's configure the verticle to listen on the 'test' port (randomly picked).
    // We create deployment options and set the _configuration_ json object:
    //    ServerSocket socket = new ServerSocket(0);
    //    port = socket.getLocalPort();
    //    socket.close();

    setProperty(LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory::class.java.name)
    LoggerFactory.getLogger(LoggerFactory::class.java) // Required for Logback to work in Vertx

    Guice.createInjector(Example1Module(vertx)).injectMembers(this)

    val options = DeploymentOptions()
            .setConfig(JsonObject().put("http.port", port)
            )

    vertx.deployVerticle(projectionVerticle, options, context.asyncAssertSuccess())
    vertx.deployVerticle(restVerticle, options, context.asyncAssertSuccess())
    vertx.deployVerticle(cmdVerticle, options, context.asyncAssertSuccess())

    val h = jdbi!!.open()
    h.createScript("DELETE FROM units_of_work").execute()
    h.createScript("DELETE FROM customer_summary").execute()
    h.commit()
  }

  /**
   * This method, called after our test, just cleanup everything by closing the vert.x instance
   *
   * @param context the test context
   */
  @After
  fun tearDown(context: TestContext) {
    vertx.close(context.asyncAssertSuccess())
  }

  // tag::create_customer_test[]

  @Test
  fun create_customer(context: TestContext) {

    // This test is asynchronous, so get an async handler to inform the test when we are done.
    val async = context.async()

    val customerId = CustomerId(UUID.randomUUID().toString())
    val createCustomerCmd = CreateCustomer(UUID.randomUUID(), customerId, "customer-test")
    val expectedEvent = CustomerCreated(customerId, "customer-test")
    val expectedUow = EntityUnitOfWork(UUID.randomUUID(), createCustomerCmd, Version(1),
            listOf<DomainEvent>(expectedEvent))

    val json = Json.encodePrettily(createCustomerCmd)

    vertx.createHttpClient().put(port, "localhost",
      "/" + aggregateRootId(Customer::class.java) + "/commands")
      .putHeader("content-type", "application/json")
      .putHeader("content-length", Integer.toString(json.length))
      .handler { response ->
        context.assertEquals(response.statusCode(), 201)
        context.assertTrue(response.headers().get("content-type").equals("application/json"))
        response.bodyHandler { body ->
          println("---> body " + body)
          val cmdExec = Json.decodeValue(body.toString(), CommandExecution::class.java)
          context.assertEquals(cmdExec.unitOfWork.targetId(), expectedUow.targetId())
          context.assertEquals(cmdExec.unitOfWork.command, expectedUow.command)
          context.assertEquals(cmdExec.unitOfWork.events, expectedUow.events)
          context.assertEquals(cmdExec.unitOfWork.version, expectedUow.version)
          async.complete()
        }
      }
      .write(json)
      .end()
  }
  // tag::create_customer_test[]

}
