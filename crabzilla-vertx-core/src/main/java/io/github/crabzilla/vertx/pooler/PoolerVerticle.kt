package io.github.crabzilla.vertx.pooler

import io.github.crabzilla.vertx.CrabzillaVerticle
import io.github.crabzilla.vertx.UnitOfWorkRepository
import io.github.crabzilla.vertx.VerticleRole.POOLER
import io.github.crabzilla.vertx.projector.ProjectionData
import io.vertx.core.Context
import io.vertx.core.Future
import io.vertx.core.Vertx
import org.slf4j.LoggerFactory


class PoolerVerticle(override val name: String,
                     val repo: UnitOfWorkRepository,
                     val intervalMs: Long): CrabzillaVerticle(name, POOLER) {

  @Throws(Exception::class)
  override fun start() {
    log.info("** starting a pooler with name= ${name} with interval (ms)= ${intervalMs}")
  }

  override fun init(vertx: Vertx?, context: Context?) {
    super.init(vertx, context)

    log.info("** init")

    vertx!!.setPeriodic(intervalMs, {

      log.info("scheduling ")

      // TODO http://www.davsclaus.com/2013/08/apache-camel-212-backoff-support-for.html

      vertx.sharedData().getClusterWideMap<String, Long>(name) { res ->

        val dataMap = res.result()

        dataMap.get(name, { result1 ->

          run {

            if (result1.failed()) {
              log.error("when pulling form events ", result1.cause())
              return@get
            }

            val counter = result1.result() ?: 0
            val selectAfterUowSeqFuture: Future<List<ProjectionData>> = Future.future<List<ProjectionData>>()

            repo.selectAfterUowSequence(counter, 1000, selectAfterUowSeqFuture)

            selectAfterUowSeqFuture.setHandler { listOfAfterUowSeqFuture ->

              run {

                if (listOfAfterUowSeqFuture.failed()) {
                  log.error("when pulling form events ", listOfAfterUowSeqFuture.cause())
                  return@run
                }

                val list = selectAfterUowSeqFuture.result()

                list.forEach { pd ->
                  log.info("will publish ${pd} to " + name)
                  dataMap.put(name, pd.uowSequence, { _ ->
                    run {
                      vertx.eventBus().publish(name, pd)
                    }
                  })
                }

              }
            }

          }

        })
      }
    })

  }

  companion object {
    internal var log = LoggerFactory.getLogger(PoolerVerticle::class.java)
  }

}
