package io.github.crabzilla.example1

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.github.crabzilla.example1.customer.CustomerModule
import io.github.crabzilla.example1.impl.CustomerSummaryProjector
import io.github.crabzilla.example1.impl.CustomerSummaryProjectorDao
import io.github.crabzilla.example1.impl.SampleInternalServiceImpl
import io.github.crabzilla.vertx.modules.JdbiModule
import io.github.crabzilla.vertx.modules.ProjectionDbModule
import io.github.crabzilla.vertx.qualifiers.ProjectionDatabase
import io.github.crabzilla.vertx.projector.JdbiProjectorVerticle
import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.core.Vertx
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import javax.inject.Singleton

@Module(includes = [JdbiModule::class, ProjectionDbModule::class, CustomerModule::class])
class Example1Module {

  @Provides @IntoSet
  fun eventsProjectorVerticle(@ProjectionDatabase jdbi: Jdbi, vertx: Vertx): JdbiProjectorVerticle<out Any> {
    val daoFactory: (Handle, Class<CustomerSummaryProjectorDao>) -> CustomerSummaryProjectorDao = {
      handle, daoClass -> handle.attach(daoClass)
    }
    val projector = CustomerSummaryProjector(CustomerSummary::class.simpleName!!, jdbi, daoFactory)
    val circuitBreaker = CircuitBreaker.create("example1-projector-circuit-breaker", vertx)
    return JdbiProjectorVerticle(subDomainName(), projector, circuitBreaker)
  }


  @Provides
  @Singleton
  fun service(): SampleInternalService {
    return SampleInternalServiceImpl()
  }

}
