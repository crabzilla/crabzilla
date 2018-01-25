package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import io.github.crabzilla.vertx.configHandler
import io.github.crabzilla.vertx.deployVerticles
import io.github.crabzilla.vertx.deployVerticlesByName
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
import joptsimple.OptionParser
import java.net.InetAddress

// tag::launcher[]

class RestServiceLauncher {

  companion object {

    val log = org.slf4j.LoggerFactory.getLogger(RestServiceLauncher::class.java.simpleName)

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
      val vertxOptions = VertxOptions().setClusterManager(mgr).setClusterHost(hostName)

      println("** hostname ${hostName}")

      Vertx.clusteredVertx(vertxOptions) { res ->
        if (res.succeeded()) {

          val vertx = res.result()

          val defaultConfigFile = RestServiceLauncher::class.java.classLoader
            .getResource("conf/config.properties").path

          configHandler(vertx, configFile, defaultConfigFile, { config ->

            log.info("config = {}", config.encodePrettily())

            vertx.executeBlocking<Any>({ future ->

              val app = DaggerRestServiceComponent.builder()
                .restServiceModule(RestServiceModule(vertx, config))
                .build()

              ds = app.datasource()

              deployVerticles(vertx, app.restVerticles())

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
