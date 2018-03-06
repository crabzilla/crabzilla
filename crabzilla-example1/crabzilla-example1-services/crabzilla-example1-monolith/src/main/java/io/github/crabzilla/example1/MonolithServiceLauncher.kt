package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import io.github.crabzilla.vertx.*
import io.vertx.core.Vertx
import io.vertx.kotlin.core.DeploymentOptions
import org.slf4j.LoggerFactory
import java.net.InetAddress

// tag::launcher[]

class MonolithServiceLauncher {

  companion object {

    val log = LoggerFactory.getLogger(MonolithServiceLauncher::class.java.simpleName)

    lateinit var writeDs: HikariDataSource
    lateinit var readDs: HikariDataSource

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {

      val hostName = InetAddress.getLocalHost().hostName

      log.info("** hostname $hostName")

      val vertx = Vertx.vertx()

      configHandler(vertx, { config ->

        log.info("config = {}", config.encodePrettily())

        vertx.executeBlocking<Any>({ future ->

          val app = DaggerMonolithServiceComponent.builder()
            .crabzillaWebModule(CrabzillaWebModule(vertx, config))
            .example1HandlerModule(Example1HandlerModule())
            .example1ProjectorModule(Example1ProjectorModule())
            .monolithServiceModule(MonolithServiceModule(vertx, config))
            .build()

          writeDs = app.writeDatasource()
          readDs = app.readDatasource()

          vertx.registerVerticleFactory(CrabzillaVerticleFactory(app.handlerVerticles(), VerticleRole.HANDLER))
          val deploymentOptions = io.vertx.core.DeploymentOptions().setHa(true)
          deployVerticlesByName(vertx, setOf(VerticleRole.HANDLER.verticle(CommandHandlers.CUSTOMER.name)), deploymentOptions)

          vertx.registerVerticleFactory(CrabzillaVerticleFactory(app.projectorVerticles(), VerticleRole.PROJECTOR))
          val workerDeploymentOptions = DeploymentOptions().setHa(true).setWorker(true)
          deployVerticlesByName(vertx, setOf(VerticleRole.PROJECTOR.verticle(subDomainName())), workerDeploymentOptions)

          deployVerticles(vertx, setOf(app.restVerticles()))

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

    }

  }

}
