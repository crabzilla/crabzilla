package io.github.crabzilla.example1.customer

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.github.crabzilla.core.Entity
import io.github.crabzilla.core.Snapshot
import io.github.crabzilla.core.SnapshotPromoter
import io.github.crabzilla.core.StateTransitionsTracker
import io.github.crabzilla.example1.CommandHandlers
import io.github.crabzilla.example1.SampleInternalService
import io.github.crabzilla.vertx.UnitOfWorkRepository
import io.github.crabzilla.vertx.handler.CommandHandlerVerticle
import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.core.Vertx
import net.jodah.expiringmap.ExpiringMap

// tag::module[]
@Module
class CustomerModule {

  @Provides @IntoSet
  fun handlerVerticle(service: SampleInternalService,
                      eventJournal: UnitOfWorkRepository,
                      vertx: Vertx): CommandHandlerVerticle<out Entity> {

    val customer = Customer(sampleInternalService = service)
    val stateTransitionFn = StateTransitionFn()
    val validator = CommandValidatorFn()

    val cache: ExpiringMap<String, Snapshot<Customer>> = ExpiringMap.create()
    val circuitBreaker = CircuitBreaker.create(CommandHandlers.CUSTOMER.name, vertx)
    val trackerFactory : (Snapshot<Customer>) -> StateTransitionsTracker<Customer> =
            { instance -> StateTransitionsTracker(instance, stateTransitionFn) }
    val cmdHandler = CommandHandlerFn(trackerFactory)
    val snapshotPromoter = SnapshotPromoter<Customer>
    { instance -> StateTransitionsTracker(instance, stateTransitionFn) }
    return CommandHandlerVerticle(CommandHandlers.CUSTOMER.name, customer, cmdHandler, validator,
      snapshotPromoter, eventJournal, cache, circuitBreaker)
  }

}
// end::module[]
