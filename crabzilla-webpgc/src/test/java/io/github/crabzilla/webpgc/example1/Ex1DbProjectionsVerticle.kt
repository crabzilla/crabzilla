package io.github.crabzilla.webpgc.example1

import io.github.crabzilla.example1.aggregate.CustomerJsonAware
import io.github.crabzilla.pgc.example1.CustomerSummaryProjector
import io.github.crabzilla.webpgc.DbProjectionsVerticle
import io.vertx.core.Future

open class Ex1DbProjectionsVerticle : DbProjectionsVerticle() {

  override fun start(startFuture: Future<Void>) {
    super.start()
    addEntityJsonAware("customer", CustomerJsonAware())
    addProjector("customers-summary", CustomerSummaryProjector())
    startFuture.complete()
  }

}
