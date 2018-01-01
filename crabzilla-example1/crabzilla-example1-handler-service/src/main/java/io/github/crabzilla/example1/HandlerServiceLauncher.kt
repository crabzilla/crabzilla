package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import io.github.crabzilla.example1.customer.ActivateCustomer
import io.github.crabzilla.example1.customer.CreateCustomer
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerId
import io.github.crabzilla.vertx.EntityCommandExecution
import io.github.crabzilla.vertx.configHandler
import io.github.crabzilla.vertx.helpers.StringHelper.cmdHandlerEndpoint
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.logging.LoggerFactory
import io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME
import io.vertx.core.logging.SLF4JLogDelegateFactory
import joptsimple.OptionParser
import java.lang.System.setProperty
import java.util.*

// tag::launcher[]

class HandlerServiceLauncher {

  companion object {

    val log = org.slf4j.LoggerFactory.getLogger(HandlerServiceLauncher::class.java.simpleName)

    lateinit var ds: HikariDataSource

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {

      setProperty(LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory::class.java.name)
      LoggerFactory.getLogger(LoggerFactory::class.java) // Required for Logback to work in Vertx

      val parser = OptionParser()
      parser.accepts("conf").withRequiredArg()
      parser.allowsUnrecognizedOptions()

      val options = parser.parse(*args)
      val configFile = options.valueOf("conf") as String?
      val vertx = Vertx.vertx()

      configHandler(vertx, configFile, { config ->

        val app = DaggerHandlerServiceComponent.builder()
                .handlerServiceModule(HandlerServiceModule(vertx, config))
                .build()

        ds = app.datasource()

        app.commandVerticles().forEach({
          vertx.deployVerticle(it) { event ->
            log.info("cmd verticle: $it.toString()")
            if (!event.succeeded()) log.error("Error deploying verticle", event.cause()) }
        })

        app.restVerticles().forEach({
          vertx.deployVerticle(it) { event ->
            log.info("rest verticle: $it.toString()")
            if (!event.succeeded()) log.error("Error deploying verticle", event.cause()) }
        })

        // just a test
        justForTest(vertx)

      }, {
        ds.close()
      })

    }

  }

}

// end::launcher[]

fun justForTest(vertx: Vertx) {

  val customerId = CustomerId(UUID.randomUUID().toString())
  //    val customerId = new CustomerId("customer123");
  val createCustomerCmd = CreateCustomer(UUID.randomUUID(), customerId, "a good customer")
  val options = DeliveryOptions().setCodecName("EntityCommand")

  // create customer command
  vertx.eventBus().send<EntityCommandExecution>(cmdHandlerEndpoint(Customer::class.java), createCustomerCmd, options) { asyncResult ->

    HandlerServiceLauncher.log.info("Successful create customer test? {}", asyncResult.succeeded())

    if (asyncResult.succeeded()) {

      HandlerServiceLauncher.log.info("Result: {}", asyncResult.result().body())

      val activateCustomerCmd = ActivateCustomer(UUID.randomUUID(), createCustomerCmd.targetId, "because I want it")

      // activate customer command
      vertx.eventBus().send<EntityCommandExecution>(cmdHandlerEndpoint(Customer::class.java), activateCustomerCmd, options) { asyncResult2 ->

        HandlerServiceLauncher.log.info("Successful activate customer test? {}", asyncResult2.succeeded())

        if (asyncResult2.succeeded()) {
          HandlerServiceLauncher.log.info("Result: {}", asyncResult2.result().body())
        } else {
          HandlerServiceLauncher.log.info("Cause: {}", asyncResult2.cause())
          HandlerServiceLauncher.log.info("Message: {}", asyncResult2.cause().message)
        }

      }

    } else {
      HandlerServiceLauncher.log.info("Cause: {}", asyncResult.cause())
      HandlerServiceLauncher.log.info("Message: {}", asyncResult.cause().message)
    }

  }

}
