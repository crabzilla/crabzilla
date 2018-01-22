package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import io.github.crabzilla.vertx.configHandler
import io.github.crabzilla.vertx.deployVerticles
import io.github.crabzilla.vertx.projection.ProjectorVerticleFactory
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
import joptsimple.OptionParser
import java.util.concurrent.atomic.AtomicLong


// tag::launcher[]

class ProjectorServiceBareLauncher : AbstractVerticle() {

  companion object {

    val log = org.slf4j.LoggerFactory.getLogger(ProjectorServiceBareLauncher::class.java.simpleName)

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

      val vertxOptions = VertxOptions().setClusterManager(mgr).setHAEnabled(true).setHAGroup("crabzilla:projector")

      println("**  HA group" + vertxOptions.haGroup)

      Vertx.clusteredVertx(vertxOptions) { res ->
        if (res.succeeded()) {

          val vertx = res.result()

          val defaultConfigFile = ProjectorServiceBareLauncher::class.java.classLoader
            .getResource("conf/config.properties").path

          configHandler(vertx, configFile, defaultConfigFile, { config ->

            log.info("config = {}", config.encodePrettily())

            val component = DaggerProjectorServiceComponent.builder()
              .projectorServiceModule(ProjectorServiceModule(vertx, config))
              .build()

            val poolerVerticle = PoolerVerticle(component.projectionRepo(), 10000, AtomicLong(0)) // TODO get from db

            vertx.registerVerticleFactory(PoolerVerticleFactory(setOf(poolerVerticle)))
            vertx.registerVerticleFactory(ProjectorVerticleFactory(component.projectorVerticles()))

            deployVerticles(vertx, setOf(ProjectorServiceBareLauncher()))

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

