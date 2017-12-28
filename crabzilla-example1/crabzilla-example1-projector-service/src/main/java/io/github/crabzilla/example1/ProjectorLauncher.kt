package io.github.crabzilla.example1

import io.github.crabzilla.vertx.helpers.ConfigHelper.cfgOptions
import io.vertx.config.ConfigRetriever
import io.vertx.core.Vertx
import io.vertx.core.logging.LoggerFactory
import io.vertx.core.logging.LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME
import io.vertx.core.logging.SLF4JLogDelegateFactory
import joptsimple.OptionParser
import java.lang.System.setProperty

// tag::launcher[]

class ProjectorLauncher {

  companion object {

    val log = org.slf4j.LoggerFactory.getLogger(ProjectorLauncher::class.java.simpleName)

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {

      setProperty(LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory::class.java.name)
      LoggerFactory.getLogger(LoggerFactory::class.java) // Required for Logback to work in Vertx

      val parser = OptionParser()
      parser.accepts("conf").withRequiredArg()
      parser.allowsUnrecognizedOptions()

      val options = parser.parse(*args)
      val configFile = options.valueOf("conf") as String?
      val vertx = Vertx.vertx()
      val retriever = ConfigRetriever.create(vertx, cfgOptions(configFile))

      retriever.getConfig { ar ->

        if (ar.failed()) {
          log.error("failed to load config", ar.cause())
          return@getConfig
        }

        val config = ar.result()
        log.info("config = {}", config.encodePrettily())

        val app = DaggerProjectorComponent.builder()
                .projectorServiceModule(ProjectorServiceModule(vertx, config))
                .build()

        app.projectorVerticles().forEach({
          vertx.deployVerticle(it) { event ->
            log.info("rest verticle: $it.toString()")
            if (!event.succeeded()) log.error("Error deploying verticle", event.cause()) }
        })

      }

    }

  }

}

// end::launcher[]

