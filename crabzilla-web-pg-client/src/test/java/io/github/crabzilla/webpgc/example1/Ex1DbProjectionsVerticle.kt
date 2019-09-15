package io.github.crabzilla.webpgc.example1

import io.github.crabzilla.example1.customer.CustomerJsonAware
import io.github.crabzilla.pgc.example1.CustomerSummaryProjector
import io.github.crabzilla.webpgc.DbProjectionsVerticle
import io.vertx.core.Promise

open class Ex1DbProjectionsVerticle : DbProjectionsVerticle() {

  override fun start(promise: Promise<Void>) {
    super.start()
    addEntityJsonAware("customer", CustomerJsonAware())
    addProjector("customers-summary", CustomerSummaryProjector())
    promise.complete()
  }

}
