package crabzilla.example1

import com.google.inject.Guice
import crabzilla.example1.customer.ActivateCustomer
import crabzilla.example1.customer.CreateCustomer
import crabzilla.example1.customer.Customer
import crabzilla.example1.customer.CustomerId
import crabzilla.stack.CommandExecution
import crabzilla.stack.StringHelper.commandHandlerId
import crabzilla.vertx.verticles.CommandHandlerVerticle
import crabzilla.vertx.verticles.CommandRestVerticle
import crabzilla.vertx.verticles.EventsProjectionVerticle
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.logging.LoggerFactory
import io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME
import io.vertx.core.logging.SLF4JLogDelegateFactory
import mu.KotlinLogging
import java.lang.System.setProperty
import java.util.*
import javax.inject.Inject

class Example1Launcher {

  val log = KotlinLogging.logger {}

//  @Inject
//  internal lateinit var aggregateRootVerticles: Map<String, Verticle>

  @Inject
  internal lateinit var projectionVerticle: EventsProjectionVerticle<CustomerSummaryDao>

  @Inject
  internal lateinit var restVerticle: CommandRestVerticle<Customer>

  @Inject
  internal lateinit var cmdVerticle: CommandHandlerVerticle<Customer>


  private fun justForTest() {

    val customerId = CustomerId(UUID.randomUUID().toString())
    // val customerId = new CustomerId("customer-000");
    val createCustomerCmd = CreateCustomer(UUID.randomUUID(), customerId, "a good customer")
    val options = DeliveryOptions().setCodecName("EntityCommand")

    // create customer command
    vertx.eventBus().send<CommandExecution>(commandHandlerId(Customer::class.java), createCustomerCmd, options) { asyncResult ->

      log.info("Successful create customer test? {}", asyncResult.succeeded())

      if (asyncResult.succeeded()) {

        log.info("Result: {}", asyncResult.result().body())

        val activateCustomerCmd = ActivateCustomer(UUID.randomUUID(), createCustomerCmd._targetId, "because I want it")

        log.info("-----> Will send : {}", activateCustomerCmd)

        // activate customer command
        vertx.eventBus().send<CommandExecution>(commandHandlerId(Customer::class.java), activateCustomerCmd, options) { asyncResult2 ->

          log.info("Successful activate customer test? {}", asyncResult2.succeeded())

          if (asyncResult2.succeeded()) {
            log.info("Result: {}", asyncResult2.result().body())
          } else {
            log.info("Cause: {}", asyncResult2.cause())
            log.info("Message: {}", asyncResult2.cause().message)
          }

        }

      } else {
        log.info("Cause: {}", asyncResult.cause())
        log.info("Message: {}", asyncResult.cause().message)
      }

    }

  }

  companion object {

    val log = KotlinLogging.logger {}

    internal lateinit var vertx: Vertx

    @Throws(InterruptedException::class)
    @JvmStatic
    fun main(args: Array<String>) {

      val launcher = Example1Launcher()

      vertx = Vertx.vertx()

      setProperty(LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory::class.java.name)
      LoggerFactory.getLogger(LoggerFactory::class.java) // Required for Logback to work in Vertx

      Guice.createInjector(Example1Module(vertx)).injectMembers(launcher)

//          for ((key, value) in launcher.aggregateRootVerticles!!) {
//            vertx.deployVerticle(value) { event -> log.debug("Deployed {} ? {}", key, event.succeeded()) }
//          }

      vertx.deployVerticle(launcher.projectionVerticle) { event -> log.debug("Deployed ? {}", event.succeeded()) }

      vertx.deployVerticle(launcher.restVerticle) { event -> log.debug("Deployed ? {}", event.succeeded()) }

      vertx.deployVerticle(launcher.cmdVerticle) { event -> log.debug("Deployed ? {}", event.succeeded()) }

      // a test
      launcher.justForTest()

    }
  }

}
