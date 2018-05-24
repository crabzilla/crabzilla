package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import io.github.crabzilla.vertx.*
import io.github.crabzilla.vertx.VerticleRole.PROJECTOR
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
import java.net.InetAddress


class ProjectorServiceLauncher {

  companion object {

    private val log = org.slf4j.LoggerFactory.getLogger(ProjectorServiceLauncher::class.java.simpleName)

    lateinit var ds: HikariDataSource

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {

      val hostName = InetAddress.getLocalHost().hostName
      val mgr = HazelcastClusterManager()
      val vertxOptions = VertxOptions().setClusterManager(mgr).setHAEnabled(true).setHAGroup("events-projector")
                                       .setClusterHost(hostName)

      println("**  HA group ${vertxOptions.haGroup} hostname ${hostName}")

      Vertx.clusteredVertx(vertxOptions) { res ->
        if (res.succeeded()) {

          val vertx = res.result()

          val envOptions = ConfigStoreOptions().setType("env")

          configHandler(vertx, envOptions, { config ->

            log.info("config = {}", config.encodePrettily())

            vertx.executeBlocking<Any>({ future ->

              val component = DaggerProjectorServiceComponent.builder()
                .projectorServiceModule(ProjectorServiceModule(vertx, config))
                .build()

              ds = component.datasource()

              val workerDeploymentOptions = DeploymentOptions().setHa(true).setWorker(true)
              deployVerticles(vertx, setOf(component.healthVerticle()), workerDeploymentOptions)
              vertx.registerVerticleFactory(CrabzillaVerticleFactory(component.projectorVerticles(), PROJECTOR))
              deployVerticlesByName(vertx, setOf(PROJECTOR.verticle(subDomainName())), workerDeploymentOptions)

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

