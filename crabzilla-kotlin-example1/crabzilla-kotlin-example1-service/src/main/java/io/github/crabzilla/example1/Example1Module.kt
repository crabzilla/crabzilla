package io.github.crabzilla.example1

import com.google.inject.Provides
import io.github.crabzilla.example1.services.SampleInternalServiceImpl
import io.github.crabzilla.vertx.projection.EventProjector
import io.github.crabzilla.vertx.projection.EventsProjectionVerticle
import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.circuitbreaker.CircuitBreakerOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.jdbi.v3.core.Jdbi
import javax.inject.Singleton

class Example1Module(private val vertx: Vertx, private val config: JsonObject) : CrabzillaModule(vertx, config) {

  override fun configure() {

    configureVertx()

    bind(CustomerRepository::class.java).to(CustomerRepositoryImpl::class.java).asEagerSingleton()

    // services
    bind(SampleInternalService::class.java).to(SampleInternalServiceImpl::class.java).asEagerSingleton()

  }

  @Provides
  @Singleton
  fun customerSummaryDao(jdbi: Jdbi) : CustomerSummaryDao {
    return jdbi.onDemand(CustomerSummaryDao::class.java)
  }

  @Provides
  @Singleton
  fun eventsProjectorVerticle(jdbi: Jdbi,
                              eventsProjector: EventProjector<CustomerSummaryDao>): EventsProjectionVerticle<CustomerSummaryDao> {
    val circuitBreaker = CircuitBreaker.create("events-projection-circuit-breaker", vertx,
            CircuitBreakerOptions()
                    .setMaxFailures(5) // number SUCCESS failure before opening the circuit
                    .setTimeout(2000) // consider a failure if the operation does not succeed in time
                    .setFallbackOnFailure(true) // do we call the fallback on failure
                    .setResetTimeout(10000) // time spent in open state before attempting to re-try
    )
    return EventsProjectionVerticle(eventsProjector, circuitBreaker)
  }

  @Provides
  @Singleton
  fun eventsProjector(jdbi: Jdbi): EventProjector<CustomerSummaryDao> {
    return Example1EventProjector("example1", CustomerSummaryDao::class.java, jdbi)
  }

}
