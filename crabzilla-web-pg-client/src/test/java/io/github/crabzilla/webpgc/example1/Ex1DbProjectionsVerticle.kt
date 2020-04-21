package io.github.crabzilla.webpgc.example1

import io.github.crabzilla.example1.example1Json
import io.github.crabzilla.pgc.example1.CustomerSummaryProjector
import io.github.crabzilla.webpgc.DbProjectionsVerticle
import io.vertx.core.Promise

open class Ex1DbProjectionsVerticle : DbProjectionsVerticle() {

  override fun start(promise: Promise<Void>) {
    super.start()
    addProjector("customers-summary", CustomerSummaryProjector(), example1Json)
    promise.complete()
  }

}
