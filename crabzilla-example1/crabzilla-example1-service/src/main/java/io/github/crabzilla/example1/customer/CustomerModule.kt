package io.github.crabzilla.example1.customer

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.entity.*
import io.github.crabzilla.example1.SampleInternalService
import io.github.crabzilla.vertx.entity.EntityCommandHandlerVerticle
import io.github.crabzilla.vertx.entity.EntityCommandRestVerticle
import io.github.crabzilla.vertx.entity.EntityUnitOfWorkRepository
import io.github.crabzilla.vertx.modules.qualifiers.WriteDatabase
import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import net.jodah.expiringmap.ExpiringMap
import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Supplier
import javax.inject.Singleton


// tag::module[]
@Module
class CustomerModule(val vertx: Vertx, val config: JsonObject) {

  @Provides @IntoMap
  @StringKey("rest")
  fun restVerticle(uowRepository: EntityUnitOfWorkRepository): Verticle {
    return EntityCommandRestVerticle(Customer::class.java, config, uowRepository)
  }

  @Provides @IntoMap
  @StringKey("commander")
  fun handlerVerticle(supplier: Supplier<Customer>,
                      cmdHandler: (EntityCommand, Snapshot<Customer>) -> EntityCommandResult,
                      validator: (EntityCommand) -> List<String>,
                      snapshotPromoter: SnapshotPromoter<Customer>,
                      eventRepository: EntityUnitOfWorkRepository): Verticle {
    val mycache: ExpiringMap<String, Snapshot<Customer>> = ExpiringMap.create()
    val circuitBreaker = CircuitBreaker.create("command-handler-circuit-breaker", vertx)
    return EntityCommandHandlerVerticle(Customer::class.java, supplier.get(), cmdHandler, validator, snapshotPromoter,
            eventRepository, mycache, circuitBreaker)
  }

  @Provides
  @Singleton
  fun supplierFn(service: SampleInternalService): Supplier<Customer> {
    return Supplier { Customer(sampleInternalService = service) }
  }

  @Provides
  @Singleton
  fun stateTransitionFn(): (DomainEvent, Customer) -> Customer {
    return StateTransitionFn()
  }

  @Provides
  @Singleton
  fun cmdValidatorFn(): (EntityCommand) -> List<String> {
    return CommandValidatorFn()
  }

  @Provides
  @Singleton
  fun cmdHandlerFn(stateTransitionFn: (DomainEvent, Customer) -> Customer):
          (EntityCommand, Snapshot<Customer>) -> EntityCommandResult {
    val trackerFactory : (Snapshot<Customer>) -> StateTransitionsTracker<Customer> =
            { instance -> StateTransitionsTracker(instance, stateTransitionFn)
    }
    return CommandHandlerFn(trackerFactory)
  }

  @Provides
  @Singleton
  fun snapshotPromoter(stateTransitionFn: (DomainEvent, Customer) -> Customer): SnapshotPromoter<Customer> {
    return SnapshotPromoter { instance -> StateTransitionsTracker(instance, stateTransitionFn) }
  }


  @Provides
  @Singleton
  fun customerRepo(@WriteDatabase jdbcClient: JDBCClient): EntityUnitOfWorkRepository {
    return EntityUnitOfWorkRepository(Customer::class.java, jdbcClient)
  }


}
// end::module[]