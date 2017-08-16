//package crabzilla.example1.customer
//
//import crabzilla.example1.SampleInternalService
//import crabzilla.model.*
//import crabzilla.vertx.AggregateRootComponentsFactory
//import io.vertx.core.Vertx
//import io.vertx.ext.jdbc.JDBCClient
//import java.util.function.BiFunction
//import java.util.function.Supplier
//import javax.inject.Inject
//
//class KCustomerFactory @Inject constructor(private val internalService: SampleInternalService,
//                                           private val vertx: Vertx, private val jdbcClient: JDBCClient)
//  : AggregateRootComponentsFactory<Customer> {
//
//  override fun supplierFn(): Supplier<Customer> {
//    val seedValue = depInjectionFn().apply { Customer() }
//    return Supplier { seedValue.apply(Customer()) }
//  }
//
//  override fun clazz(): Class<Customer> {
//    return Customer::class.java
//  }
//
//  override fun stateTransitionFn(): BiFunction<DomainEvent, Customer, Customer> {
//    return CustomerStateTransitionFn()
//  }
//
//  override fun cmdValidatorFn(): java.util.function.Function<EntityCommand, List<String>> {
//    return CustomerCommandValidatorFn()
//  }
//
//  override fun cmdHandlerFn(): BiFunction<EntityCommand, Snapshot<Customer>, CommandHandlerResult> {
//    val trackerFactory = StateTransitionsTrackerFactory<Customer> {
//      instance ->
//      StateTransitionsTracker(instance, stateTransitionFn(), depInjectionFn())
//    }
//    return CustomerCommandHandlerFn(trackerFactory)
//  }
//
//  override fun depInjectionFn(): java.util.function.Function<Customer, Customer> {
//    return Function { customer -> customer.copy(sampleInternalService = internalService) }
//  }
//
//  override fun vertx(): Vertx {
//    return vertx
//  }
//
//  override fun jdbcClient(): JDBCClient {
//    return jdbcClient
//  }
//
//}