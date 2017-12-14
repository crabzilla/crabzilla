package io.github.crabzilla.example1.customer


import io.github.crabzilla.core.DomainEvent
import io.github.crabzilla.core.entity.*
import io.github.crabzilla.example1.SampleInternalService
import io.github.crabzilla.vertx.entity.roles.AggregateComponentsFactory
import io.vertx.core.Vertx
import io.vertx.ext.jdbc.JDBCClient
import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Supplier
import javax.inject.Inject
import javax.inject.Singleton

// tag::factory[]
@Singleton
class CustomerFactory @Inject
constructor(service: SampleInternalService, private val vertx: Vertx,
            private val jdbcClient: JDBCClient) : AggregateComponentsFactory<Customer> {

  private val seedValue by lazy { Customer(sampleInternalService = service) }

  override fun clazz(): Class<Customer> {
    return Customer::class.java
  }

  override fun supplierFn(): Supplier<Customer> {
    return Supplier { seedValue }
  }

  override fun stateTransitionFn(): BiFunction<DomainEvent, Customer, Customer> {
    return StateTransitionFn()
  }

  override fun cmdValidatorFn(): Function<EntityCommand, List<String>> {
    return CommandValidatorFn()
  }

  override fun cmdHandlerFn():
          BiFunction<EntityCommand, Snapshot<Customer>, EntityCommandResult> {
    val trackerFactory = StateTransitionsTrackerFactory<Customer> { instance ->
      StateTransitionsTracker(instance, stateTransitionFn())
    }
    return CommandHandlerFn(trackerFactory)
  }

  override fun jdbcClient(): JDBCClient {
    return jdbcClient
  }

  override fun vertx(): Vertx {
    return vertx
  }

}
// end::factory[]
