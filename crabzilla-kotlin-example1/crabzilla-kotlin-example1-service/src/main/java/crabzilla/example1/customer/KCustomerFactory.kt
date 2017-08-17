package crabzilla.example1.customer


import crabzilla.example1.SampleInternalService
import crabzilla.example1.services.KSampleInternalServiceImpl
import crabzilla.model.*
import crabzilla.vertx.AggregateRootComponentsFactory
import io.vertx.core.Vertx
import io.vertx.ext.jdbc.JDBCClient
import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Supplier
import javax.inject.Inject

class KCustomerFactory @Inject
constructor(private val service: SampleInternalService, private val vertx: Vertx, private val jdbcClient: JDBCClient) : AggregateRootComponentsFactory<Customer> {


  override fun clazz(): Class<Customer> {
    return Customer::class.java
  }

  override fun supplierFn(): Supplier<Customer> {
    return Supplier { depInjectionFn().apply(Customer(null, null, false, null, KSampleInternalServiceImpl())) }
  }

  override fun stateTransitionFn(): BiFunction<DomainEvent, Customer, Customer> {
    return StateTransitionFn()
  }

  override fun cmdValidatorFn(): Function<EntityCommand, List<String>> {
    return CommandValidatorFn()
  }

  override fun cmdHandlerFn(): BiFunction<EntityCommand, Snapshot<Customer>, CommandHandlerResult> {
    val trackerFactory = StateTransitionsTrackerFactory<Customer> {
      instance ->
      StateTransitionsTracker(instance, stateTransitionFn(), depInjectionFn())
    }
    return CommandHandlerFn(trackerFactory)
  }

  override fun depInjectionFn(): Function<Customer, Customer> {
    return Function { c -> c }
  }

  override fun jdbcClient(): JDBCClient {
    return jdbcClient
  }

  override fun vertx(): Vertx {
    return vertx
  }

}
