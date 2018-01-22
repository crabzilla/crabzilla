package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import io.github.crabzilla.vertx.configHandler
import io.github.crabzilla.vertx.deployVerticles
import io.github.crabzilla.vertx.deployVerticlesByName
import io.github.crabzilla.vertx.projection.ProjectorVerticleFactory
import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
import joptsimple.OptionParser


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

      val mgr = HazelcastClusterManager()
      val vertxOptions = VertxOptions().setClusterManager(mgr).setHAEnabled(true).setHAGroup("events-projector")

      println("**  HA group" + vertxOptions.haGroup)

      Vertx.clusteredVertx(vertxOptions) { res ->
        if (res.succeeded()) {

          val vertx = res.result()

          val defaultConfigFile = ProjectorServiceLauncher::class.java.classLoader
            .getResource("conf/config.properties").path

          configHandler(vertx, configFile, defaultConfigFile, { config ->

            log.info("config = {}", config.encodePrettily())

            val component = DaggerProjectorServiceComponent.builder()
              .projectorServiceModule(ProjectorServiceModule(vertx, config))
              .build()

//            val poolerVerticle = PoolerVerticle(component.projectionRepo(), 10000, AtomicLong(0)) // TODO get from db
//            vertx.registerVerticleFactory(PoolerVerticleFactory(setOf(poolerVerticle)))

            vertx.registerVerticleFactory(ProjectorVerticleFactory(component.projectorVerticles()))

            val workerDeploymentOptions = DeploymentOptions().setHa(true).setWorker(true)

            deployVerticles(vertx, setOf(ProjectorServiceLauncher()))

            deployVerticlesByName(vertx, setOf("crabzilla-projector:example1"), workerDeploymentOptions)

//            deployVerticlesByName(vertx, setOf("crabzilla-pooler:example1"), workerDeploymentOptions)

            ds = component.datasource()

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

