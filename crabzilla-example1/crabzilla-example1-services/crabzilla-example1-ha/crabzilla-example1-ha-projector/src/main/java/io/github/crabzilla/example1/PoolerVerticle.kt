package io.github.crabzilla.example1

import io.github.crabzilla.vertx.ProjectionData
import io.github.crabzilla.vertx.projection.ProjectionRepository
import io.vertx.core.AbstractVerticle
import io.vertx.core.Context
import io.vertx.core.Future
import io.vertx.core.Vertx
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong

class PoolerVerticle(val repo: ProjectionRepository, val intervalMs: Long,
                     val counter: AtomicLong): AbstractVerticle() {

  @Throws(Exception::class)
  override fun start() {
    log.info("** start")
  }

  override fun init(vertx: Vertx?, context: Context?) {
    super.init(vertx, context)

    log.info("** init")

    vertx!!.setPeriodic(intervalMs, {

      log.info("scheduling ")

      // TODO http://www.davsclaus.com/2013/08/apache-camel-212-backoff-support-for.html
      val f: Future<List<ProjectionData>> = Future.future<List<ProjectionData>>()

      repo.selectAfterUowSequence(counter.get(), 1000, f)

      f.setHandler { r ->
        run {
          if (r.failed()) {
            log.error("when pulling form events ", r.cause())
          } else {
            val list = f.result()
            list.forEach { pd ->
              counter.getAndIncrement()
              log.info("will publish ${pd} to " + "example1-events")
              vertx.eventBus().publish("example1-events", pd)
            }

          }
        }

      }
    })

  }

  companion object {
    internal var log = LoggerFactory.getLogger(PoolerVerticle::class.java)
  }

}
