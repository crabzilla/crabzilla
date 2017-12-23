package io.github.crabzilla.example1

import io.github.crabzilla.example1.customer.*
import io.github.crabzilla.vertx.entity.EntityCommandExecution
import io.github.crabzilla.vertx.helpers.ConfigHelper.cfgOptions
import io.github.crabzilla.vertx.helpers.StringHelper.commandHandlerId
import io.vertx.config.ConfigRetriever
import io.vertx.core.Verticle
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
//        val injector = Guice.createInjector(Example1Module(vertx, config), CustomerModule())
//
//        injector.deployVerticles(vertx)
//
//        // just a test
//        launcher.justForTest(vertx)

      }

    }


  }

  private fun justForTest(vertx: Vertx) {

    val customerId = CustomerId(UUID.randomUUID().toString())
    //    val customerId = new CustomerId("customer123");
    val createCustomerCmd = CreateCustomer(UUID.randomUUID(), customerId, "a good customer")
    val options = DeliveryOptions().setCodecName("EntityCommand")

    // create customer command
    vertx.eventBus().send<EntityCommandExecution>(commandHandlerId(Customer::class.java), createCustomerCmd, options) { asyncResult ->

      log.info("Successful create customer test? {}", asyncResult.succeeded())

      if (asyncResult.succeeded()) {

        log.info("Result: {}", asyncResult.result().body())

        val activateCustomerCmd = ActivateCustomer(UUID.randomUUID(), createCustomerCmd._targetId, "because I want it")

        // activate customer command
        vertx.eventBus().send<EntityCommandExecution>(commandHandlerId(Customer::class.java), activateCustomerCmd, options) { asyncResult2 ->

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
//
//fun Injector.deployVerticles(vertx: Vertx) {
//
//  this.allBindings.filter { entry -> entry.key.typeLiteral.rawType.simpleName.endsWith("Verticle")}
//          .entries
//          .sortedWith(compareBy({ verticleDeploymentOrder(it.key.typeLiteral.rawType.simpleName) }))
//          .map { it.value.provider.get() as Verticle}
//          .forEach {
//            vertx.deployVerticle(it) { event ->
//              if (!event.succeeded()) Example1Launcher.log.error("Error deploying verticle", event.cause()) }
//          }
//
//}

fun verticleDeploymentOrder(className: String?) : Int {
  return when(className) {
    "EventsProjectionVerticle"-> 0
    "EntityCommandHandlerVerticle"-> 1
    "EntityCommandRestVerticle" -> 2
    else -> 10
  }
}