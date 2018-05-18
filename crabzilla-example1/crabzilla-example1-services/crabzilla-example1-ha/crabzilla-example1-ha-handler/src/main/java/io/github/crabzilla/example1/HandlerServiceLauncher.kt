package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import io.github.crabzilla.vertx.CrabzillaVerticleFactory
import io.github.crabzilla.vertx.VerticleRole.HANDLER
import io.github.crabzilla.vertx.configHandler
import io.github.crabzilla.vertx.deployVerticlesByName
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx.clusteredVertx
import io.vertx.core.VertxOptions
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
import java.net.InetAddress

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

      log.info("HA group ${vertxOptions.haGroup} hostname ${hostName}")

      clusteredVertx(vertxOptions) { res ->
        if (res.succeeded()) {

          val vertx = res.result()

          log.info("got vertx")

          configHandler(vertx, { config ->

            log.info("will initialize...")

            vertx.executeBlocking<Any>({ future ->

              log.info("will instantiate components...")

              val app = DaggerHandlerServiceComponent.builder()
                .handlerServiceModule(HandlerServiceModule(vertx, config))
                .build()

              ds = app.datasource()

              vertx.registerVerticleFactory(CrabzillaVerticleFactory(app.commandVerticles(), HANDLER))

              val workerDeploymentOptions = DeploymentOptions().setHa(true)

              deployVerticlesByName(vertx, setOf(HANDLER.verticle(CommandHandlers.CUSTOMER.name)), workerDeploymentOptions)

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
