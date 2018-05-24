package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import io.github.crabzilla.vertx.configHandler
import io.github.crabzilla.vertx.deployVerticles
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
import org.slf4j.LoggerFactory
import java.net.InetAddress

class RestServiceLauncher {

  companion object {

    val log = LoggerFactory.getLogger(RestServiceLauncher::class.java.simpleName)

    lateinit var writeDs: HikariDataSource
    lateinit var readDs: HikariDataSource

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {

      val hostName = InetAddress.getLocalHost().hostName
      val mgr = HazelcastClusterManager()
      val vertxOptions = VertxOptions().setClusterManager(mgr).setClusterHost(hostName)

      log.info("** hostname $hostName")

      Vertx.clusteredVertx(vertxOptions) { res ->
        if (res.succeeded()) {

          val vertx = res.result()

          val envOptions = ConfigStoreOptions().setType("env")

          configHandler(vertx, envOptions, { config ->

            log.info("config = {}", config.encodePrettily())

            vertx.executeBlocking<Any>({ future ->

              val component = DaggerRestServiceComponent.builder()
                .restServiceModule(RestServiceModule(vertx, config))
                .build()

              writeDs = component.writeDatasource()
              readDs = component.readDatasource()

              deployVerticles(vertx, setOf(component.restVerticle()))

              future.complete()

            }, { res ->

              if (res.failed()) {
                log.error("when starting component", res.cause())
              }
            })

          }, {
            writeDs.close()
            readDs.close()
          })

        } else {
          log.error("Error", res.cause())
        }
      }

    }

  }

}
