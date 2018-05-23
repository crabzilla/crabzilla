package io.github.crabzilla.example1

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.github.crabzilla.vertx.modules.JdbiModule
import io.github.crabzilla.vertx.modules.ProjectionDbModule
import io.github.crabzilla.vertx.modules.qualifiers.ProjectionDatabase
import io.github.crabzilla.vertx.projector.JdbiProjectorVerticle
import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.core.Vertx
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi

@Module(includes = [JdbiModule::class, ProjectionDbModule::class])
class Example1ProjectorModule {

  @Provides @IntoSet
  fun eventsProjectorVerticle(@ProjectionDatabase jdbi: Jdbi, vertx: Vertx): JdbiProjectorVerticle<out Any> {
    val daoFactory: (Handle, Class<CustomerSummaryProjectorDao>) -> CustomerSummaryProjectorDao = {
      handle, daoClass -> handle.attach(daoClass)
    }
    val projector = CustomerSummaryProjector(CustomerSummary::class.simpleName!!, jdbi, daoFactory)
    val circuitBreaker = CircuitBreaker.create("example1-projector-circuit-breaker", vertx)
    return JdbiProjectorVerticle(subDomainName(), projector, circuitBreaker)
  }

}
