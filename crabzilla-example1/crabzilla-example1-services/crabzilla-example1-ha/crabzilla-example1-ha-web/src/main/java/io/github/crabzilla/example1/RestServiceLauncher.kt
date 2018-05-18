package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import io.github.crabzilla.vertx.CrabzillaWebModule
import io.github.crabzilla.vertx.configHandler
import io.github.crabzilla.vertx.deployVerticles
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

          configHandler(vertx, { config ->

            log.info("config = {}", config.encodePrettily())

            vertx.executeBlocking<Any>({ future ->

              val app = DaggerRestServiceComponent.builder()
                .restServiceModule(RestServiceModule(vertx, config))
                .crabzillaWebModule(CrabzillaWebModule(vertx, config))
                .build()

              writeDs = app.writeDatasource()
              readDs = app.readDatasource()

              deployVerticles(vertx, setOf(app.restVerticle()))

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
