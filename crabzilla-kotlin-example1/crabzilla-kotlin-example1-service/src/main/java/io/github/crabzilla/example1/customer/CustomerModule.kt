package io.github.crabzilla.example1.customer

import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.entity.*
import io.github.crabzilla.example1.SampleInternalService
import io.github.crabzilla.vertx.entity.EntityCommandHandlerVerticle
import io.github.crabzilla.vertx.entity.EntityCommandRestVerticle
import io.github.crabzilla.vertx.entity.EntityUnitOfWorkRepository
import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.circuitbreaker.CircuitBreakerOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import net.jodah.expiringmap.ExpiringMap
import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Supplier
import javax.inject.Singleton


// tag::module[]
class CustomerModule {

  // @Provides
  @Singleton
  fun restVerticle(config: JsonObject, uowRepository: EntityUnitOfWorkRepository):
          EntityCommandRestVerticle<Customer> {
    return EntityCommandRestVerticle(Customer::class.java, config, uowRepository)
  }

  // @Provides
  @Singleton
  fun handlerVerticle(supplier: Supplier<Customer>,
                               cmdHandler: BiFunction<EntityCommand, Snapshot<Customer>, EntityCommandResult>,
                               validator: Function<EntityCommand, List<String>>,
                               snapshotPromoter: SnapshotPromoter<Customer>,
                               eventRepository: EntityUnitOfWorkRepository,
                               vertx: Vertx): EntityCommandHandlerVerticle<Customer> {
    val mycache: ExpiringMap<String, Snapshot<Customer>> = ExpiringMap.create()
    val circuitBreaker = CircuitBreaker.create("command-handler-circuit-breaker", vertx,
            CircuitBreakerOptions()
                    .setMaxFailures(5) // number SUCCESS failure before opening the circuit
                    .setTimeout(2000) // consider a failure if the operation does not succeed in time
                    .setFallbackOnFailure(true) // do we call the fallback on failure
                    .setResetTimeout(10000)) // time spent in open state before attempting to re-try
    return EntityCommandHandlerVerticle(Customer::class.java, supplier.get(), cmdHandler, validator, snapshotPromoter,
            eventRepository, mycache, circuitBreaker)
  }

  // @Provides
  @Singleton
  fun supplierFn(service: SampleInternalService): Supplier<Customer> {
    return Supplier { Customer(sampleInternalService = service) }
  }

  // @Provides
  @Singleton
  fun stateTransitionFn(): BiFunction<DomainEvent, Customer, Customer> {
    return StateTransitionFn()
  }

  // @Provides
  @Singleton
  fun cmdValidatorFn(): Function<EntityCommand, List<String>> {
    return CommandValidatorFn()
  }

  // @Provides
  @Singleton
  fun cmdHandlerFn(stateTransitionFn: BiFunction<DomainEvent, Customer, Customer>):
          BiFunction<EntityCommand, Snapshot<Customer>, EntityCommandResult> {
    val trackerFactory = StateTransitionsTrackerFactory<Customer> { instance ->
      StateTransitionsTracker(instance, stateTransitionFn)
    }
    return CommandHandlerFn(trackerFactory)
  }

  // @Provides
  @Singleton
  fun snapshotPromoter(stateTransitionFn: BiFunction<DomainEvent, Customer, Customer>): SnapshotPromoter<Customer> {
    return SnapshotPromoter { instance -> StateTransitionsTracker<Customer>(instance, stateTransitionFn) }
  }


  // @Provides
  @Singleton
  fun customerRepo(jdbcClient: JDBCClient): EntityUnitOfWorkRepository {
    return EntityUnitOfWorkRepository(Customer::class.java, jdbcClient)
  }


}
// end::module[]