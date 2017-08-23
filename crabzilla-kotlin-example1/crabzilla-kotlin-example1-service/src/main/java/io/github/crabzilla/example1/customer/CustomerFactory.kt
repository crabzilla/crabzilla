package io.github.crabzilla.example1.customer


import io.github.crabzilla.example1.SampleInternalService
import io.github.crabzilla.model.*
import io.github.crabzilla.vertx.AggregateRootComponentsFactory
import io.vertx.core.Vertx
import io.vertx.ext.jdbc.JDBCClient
import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Supplier
import javax.inject.Inject

// tag::factory[]
class CustomerFactory @Inject
constructor(service: SampleInternalService, private val vertx: Vertx,
            private val jdbcClient: JDBCClient) : AggregateRootComponentsFactory<Customer> {

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
          BiFunction<EntityCommand, Snapshot<Customer>, CommandHandlerResult> {
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
