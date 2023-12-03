package io.github.crabzilla.subscription//package io.github.crabzilla.stack.subscription
//
//import io.github.crabzilla.TestRepository
//import io.github.crabzilla.cleanDatabase
//import io.github.crabzilla.example1.customer.ulidFunction
//import CrabzillaContext
//import DefaultCrabzillaContextFactory
//import io.github.crabzilla.stack.command.CommandServiceApiFactory
//import io.github.crabzilla.stack.command.DefaultCommandServiceApiFactory
//import io.github.crabzilla.testDbConfig
//import io.vertx.core.Vertx
//import io.vertx.junit5.VertxTestContext
//import org.junit.jupiter.api.BeforeEach
//
//abstract class AbstractSubscriptionIT {
//
//  abstract val subscriptionName: String
//  lateinit var context : CrabzillaContext
//  lateinit var factory : CommandServiceApiFactory
//  lateinit var subsFactory : SubscriptionApiFactory
//  lateinit var testRepo: TestRepository
//
//  @BeforeEach
//  fun setup(vertx: Vertx, tc: VertxTestContext) {
//    context = DefaultCrabzillaContextFactory().new(vertx, testDbConfig, ulidFunction)
//    factory = DefaultCommandServiceApiFactory(context)
//    subsFactory = DefaultSubscriptionApiFactory(context)
//    testRepo = TestRepository(context.pgPool())
//    cleanDatabase(context.pgPool())
//      .onFailure { tc.failNow(it) }
//      .onSuccess { tc.completeNow() }
//  }
//
//}
