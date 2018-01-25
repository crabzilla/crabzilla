package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import io.github.crabzilla.vertx.configHandler
import io.github.crabzilla.vertx.deployVerticles
import io.github.crabzilla.vertx.deployVerticlesByName
import io.github.crabzilla.vertx.pooler.PoolerVerticle
import io.github.crabzilla.vertx.pooler.PoolerVerticleFactory
import io.github.crabzilla.vertx.projection.ProjectorVerticleFactory
import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
import joptsimple.OptionParser
import java.lang.Thread.sleep
import java.net.InetAddress


// tag::launcher[]

class ProjectorServiceLauncher : AbstractVerticle() {

  companion object {

    val log = org.slf4j.LoggerFactory.getLogger(ProjectorServiceLauncher::class.java.simpleName)

    lateinit var ds: HikariDataSource

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {

      val parser = OptionParser()
      parser.accepts("conf").withRequiredArg()
      parser.allowsUnrecognizedOptions()

      val options = parser.parse(*args)
      val configFile = options.valueOf("conf") as String?

      val hostName = InetAddress.getLocalHost().hostName
      val mgr = HazelcastClusterManager()
      val vertxOptions = VertxOptions().setClusterManager(mgr).setHAEnabled(true).setHAGroup("events-projector")
        .setClusterHost(hostName)

      println("**  HA group ${vertxOptions.haGroup} hostname ${hostName}")

      Vertx.clusteredVertx(vertxOptions) { res ->
        if (res.succeeded()) {

          val vertx = res.result()

          val defaultConfigFile = ProjectorServiceLauncher::class.java.classLoader
            .getResource("conf/config.properties").path

          configHandler(vertx, configFile, defaultConfigFile, { config ->

            log.info("config = {}", config.encodePrettily())

            vertx.executeBlocking<Any>({ future ->

                log.warn("*** waiting for database connection...")
                sleep(20000)

              val component = DaggerProjectorServiceComponent.builder()
                .projectorServiceModule(ProjectorServiceModule(vertx, config))
                .build()

              ds = component.datasource()

              val poolerVerticle = PoolerVerticle("example1", component.projectionRepo(), 10000)

              vertx.registerVerticleFactory(PoolerVerticleFactory(setOf(poolerVerticle)))
              vertx.registerVerticleFactory(ProjectorVerticleFactory(component.projectorVerticles()))

              val workerDeploymentOptions = DeploymentOptions().setHa(true).setWorker(true)

              deployVerticles(vertx, setOf(ProjectorServiceLauncher()))
              deployVerticlesByName(vertx, setOf("crabzilla-projector:example1"), workerDeploymentOptions)
              deployVerticlesByName(vertx, setOf("crabzilla-pooler:example1"), workerDeploymentOptions)

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

