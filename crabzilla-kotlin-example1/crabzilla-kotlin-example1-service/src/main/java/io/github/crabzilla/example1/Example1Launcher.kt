package io.github.crabzilla.example1

import com.google.inject.Guice
import com.google.inject.Inject
import io.github.crabzilla.example1.customer.*
import io.github.crabzilla.vertx.entity.EntityCommandExecution
import io.github.crabzilla.vertx.entity.EntityCommandHandlerVerticle
import io.github.crabzilla.vertx.entity.EntityCommandRestVerticle
import io.github.crabzilla.vertx.helpers.ConfigHelper.cfgOptions
import io.github.crabzilla.vertx.helpers.StringHelper
import io.github.crabzilla.vertx.projection.EventsProjectionVerticle
import io.vertx.config.ConfigRetriever
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.logging.LoggerFactory
import io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME
import io.vertx.core.logging.SLF4JLogDelegateFactory
import joptsimple.OptionParser
import mu.KotlinLogging
import java.lang.System.setProperty
import java.util.*


class Example1Launcher {

  @Inject
  internal lateinit var projectionVerticle: EventsProjectionVerticle<CustomerSummaryDao>

  @Inject
  internal lateinit var restVerticle: EntityCommandRestVerticle<Customer>

  @Inject
  internal lateinit var cmdVerticle: EntityCommandHandlerVerticle<Customer>

  companion object {

    val log = KotlinLogging.logger {}

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
      val retriever = ConfigRetriever.create(vertx, cfgOptions(configFile))

      retriever.getConfig { ar ->

        if (ar.failed()) {
          log.error("failed to load config", ar.cause())
          return@getConfig
        }

        val config = ar.result()
        log.info("config = {}", config.encodePrettily())

        val launcher = Example1Launcher()
        val injector = Guice.createInjector(Example1Module(vertx, config), CustomerModule())

        injector.injectMembers(launcher)

        vertx.deployVerticle(launcher.projectionVerticle) { event -> log.debug("Deployed ? {}", event.succeeded()) }

        vertx.deployVerticle(launcher.restVerticle) { event -> log.debug("Deployed ? {}", event.succeeded()) }

        vertx.deployVerticle(launcher.cmdVerticle) { event -> log.debug("Deployed ? {}", event.succeeded()) }

        // a test
         launcher.justForTest(vertx);

      }

    }
  }


  private fun justForTest(vertx: Vertx) {

    val customerId = CustomerId(UUID.randomUUID().toString())
    //    val customerId = new CustomerId("customer123");
    val createCustomerCmd = CreateCustomer(UUID.randomUUID(), customerId, "a good customer")
    val options = DeliveryOptions().setCodecName("EntityCommand")

    // create customer command
    vertx.eventBus().send<EntityCommandExecution>(StringHelper.commandHandlerId(Customer::class.java), createCustomerCmd, options) { asyncResult ->

      log.info("Successful create customer test? {}", asyncResult.succeeded())

      if (asyncResult.succeeded()) {

        log.info("Result: {}", asyncResult.result().body())

        val activateCustomerCmd = ActivateCustomer(UUID.randomUUID(), createCustomerCmd._targetId, "because I want it")

        // activate customer command
        vertx.eventBus().send<EntityCommandExecution>(StringHelper.commandHandlerId(Customer::class.java), activateCustomerCmd, options) { asyncResult2 ->

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


}
