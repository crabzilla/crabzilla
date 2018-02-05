package io.github.crabzilla.example1

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.github.crabzilla.vertx.projection.ProjectionHandlerVerticle
import io.github.crabzilla.vertx.modules.qualifiers.ProjectionDatabase
import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.core.Vertx
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi

@Module
class ProjectorModule {

  @Provides @IntoSet
  fun eventsProjectorVerticle(@ProjectionDatabase jdbi: Jdbi, vertx: Vertx): ProjectionHandlerVerticle<out Any> {
    val daoFactory: (Handle, Class<CustomerSummaryProjectorDao>) -> CustomerSummaryProjectorDao = {
      handle, daoClass -> handle.attach(daoClass)
    }
    val projector = CustomerSummaryProjector(CustomerSummary::class.simpleName!!, jdbi, daoFactory)
    val circuitBreaker = CircuitBreaker.create("example1-projection-circuit-breaker", vertx)
    return ProjectionHandlerVerticle("example1", projector, circuitBreaker)
  }

}
