package io.github.crabzilla.example1

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import io.github.crabzilla.vertx.modules.qualifiers.ProjectionDatabase
import io.github.crabzilla.vertx.projection.EventsProjectionVerticle
import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import org.jdbi.v3.core.Jdbi

@Module
class ProjectorsModule(val vertx: Vertx) {

  @Provides @IntoMap
  @StringKey("projector")
  fun eventsProjectorVerticle(@ProjectionDatabase jdbi: Jdbi): Verticle {
    val projector = CustomerSummaryProjector("customer summary", CustomerSummaryProjectorDao::class.java, jdbi)
    val circuitBreaker = CircuitBreaker.create("events-projection-circuit-breaker", vertx)
    return EventsProjectionVerticle(projector, circuitBreaker)
  }

}
