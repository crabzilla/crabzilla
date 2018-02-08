package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import io.github.crabzilla.example1.customer.ActivateCustomer
import io.github.crabzilla.example1.customer.CreateCustomer
import io.github.crabzilla.example1.customer.CustomerId
import io.github.crabzilla.vertx.CrabzillaVerticleFactory
import io.github.crabzilla.vertx.VerticleRole.HANDLER
import io.github.crabzilla.vertx.configHandler
import io.github.crabzilla.vertx.deployVerticlesByName
import io.github.crabzilla.vertx.entity.EntityCommandExecution
import io.github.crabzilla.vertx.helpers.EndpointsHelper.cmdHandlerEndpoint
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
import java.net.InetAddress
import java.util.*

// tag::launcher[]

class HandlerServiceLauncher {

  companion object {

    val log = org.slf4j.LoggerFactory.getLogger(HandlerServiceLauncher::class.java.simpleName)

    lateinit var ds: HikariDataSource

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {

      val hostName = InetAddress.getLocalHost().hostName
      val mgr = HazelcastClusterManager()
      val vertxOptions = VertxOptions().setClusterManager(mgr).setHAEnabled(true).setHAGroup("command-handler")
                                       .setClusterHost(hostName)

      println("**  HA group ${vertxOptions.haGroup} hostname ${hostName}")

      Vertx.clusteredVertx(vertxOptions) { res ->
        if (res.succeeded()) {

          val vertx = res.result()

          configHandler(vertx, { config ->

            log.info("config = {}", config.encodePrettily())

            vertx.executeBlocking<Any>({ future ->

              val app = DaggerHandlerServiceComponent.builder()
                .handlerServiceModule(HandlerServiceModule(vertx, config))
                .build()

              ds = app.datasource()

              vertx.registerVerticleFactory(CrabzillaVerticleFactory(app.commandVerticles(), HANDLER))

              val workerDeploymentOptions = DeploymentOptions().setHa(true)

              deployVerticlesByName(vertx, setOf(HANDLER.verticle("Customer")), workerDeploymentOptions)
              // justForTest(vertx)
              future.complete()

            }, { res ->

              if (res.failed()) {
                log.error("when starting component", res.cause())
              }
            })

          }, {
            ds.close()
          })

        } else {
          log.error("Error", res.cause())
        }
      }

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
  vertx.eventBus().send<EntityCommandExecution>(cmdHandlerEndpoint("Customer"), createCustomerCmd, options) { asyncResult ->

    HandlerServiceLauncher.log.info("Successful create customer test? {}", asyncResult.succeeded())

    if (asyncResult.succeeded()) {

      HandlerServiceLauncher.log.info("Result: {}", asyncResult.result().body())

      val activateCustomerCmd = ActivateCustomer(UUID.randomUUID(), createCustomerCmd.targetId, "because I want it")

      // activate customer command
      vertx.eventBus().send<EntityCommandExecution>(cmdHandlerEndpoint("Customer"), activateCustomerCmd, options) { asyncResult2 ->

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
