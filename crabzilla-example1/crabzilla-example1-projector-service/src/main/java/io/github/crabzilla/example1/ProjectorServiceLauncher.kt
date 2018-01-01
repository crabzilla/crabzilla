package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import io.github.crabzilla.vertx.ProjectionData
import io.github.crabzilla.vertx.configHandler
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.core.logging.SLF4JLogDelegateFactory
import joptsimple.OptionParser

// tag::launcher[]

class ProjectorServiceLauncher {

  companion object {

    val log = org.slf4j.LoggerFactory.getLogger(ProjectorServiceLauncher::class.java.simpleName)

    lateinit var ds: HikariDataSource

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {

      System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory::class.java.name)
      LoggerFactory.getLogger(LoggerFactory::class.java) // Required for Logback to work in Vertx

      val parser = OptionParser()
      parser.accepts("conf").withRequiredArg()
      parser.allowsUnrecognizedOptions()

      val options = parser.parse(*args)
      val configFile = options.valueOf("conf") as String?
      val vertx = Vertx.vertx()

      configHandler(vertx, configFile, { config ->

        log.info("config = {}", config.encodePrettily())

        val app = DaggerProjectorServiceComponent.builder()
                .projectorServiceModule(ProjectorServiceModule(vertx, config))
                .build()

        ds = app.datasource()

        app.projectorVerticles().forEach({
          vertx.deployVerticle(it) { event ->
            log.info("projection verticle: $it.toString()")
            if (!event.succeeded()) log.error("Error deploying verticle", event.cause()) }
        })

        startEventsScanner(vertx, config, app)

      }, {
        ds.close()
      })

    }

    private fun startEventsScanner(vertx: Vertx, config: JsonObject, app: ProjectorServiceComponent) {

      vertx.setPeriodic(config.getLong("projector.interval.ms", 30000), {

        // TODO http://www.davsclaus.com/2013/08/apache-camel-212-backoff-support-for.html
        val f: Future<List<ProjectionData>> = Future.future<List<ProjectionData>>()

        app.projectionRepo().selectAfterUowSequence(0, 1000, f)

        f.setHandler { r ->
          run {
            if (r.failed()) {
              log.error("when pulling form events ", r.cause())
            } else {
              val list = f.result()
              list.forEach { pd ->
                log.info("will publish ${pd} to " + "example1-events")
                vertx.eventBus().publish("example1-events", pd)
              }

            }
          }

        }
      })
    }

  }

}

// end::launcher[]

