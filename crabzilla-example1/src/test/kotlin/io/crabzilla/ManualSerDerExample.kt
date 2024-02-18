package io.crabzilla

import io.crabzilla.context.CrabzillaContextImpl
import io.crabzilla.example1.customer.effects.CustomerWriteResultViewEffect
import io.crabzilla.example1.customer.effects.CustomersViewEffect
import io.crabzilla.example1.customer.effects.CustomersViewTrigger
import io.crabzilla.example1.customer.model.Customer
import io.crabzilla.example1.customer.model.CustomerCommand
import io.crabzilla.example1.customer.model.CustomerCommand.ActivateCustomer
import io.crabzilla.example1.customer.model.CustomerCommand.DeactivateCustomer
import io.crabzilla.example1.customer.model.CustomerCommand.RegisterCustomer
import io.crabzilla.example1.customer.model.CustomerEvent
import io.crabzilla.example1.customer.model.CustomerInitialStateFactory
import io.crabzilla.example1.customer.model.customerDecideFunction
import io.crabzilla.example1.customer.model.customerEvolveFunction
import io.crabzilla.example1.customer.serder.CustomerCommandSerDer
import io.crabzilla.example1.customer.serder.CustomerEventSerDer
import io.crabzilla.stream.TargetStream
import io.crabzilla.subscription.SubscriptionApi
import io.crabzilla.subscription.SubscriptionComponentImpl
import io.crabzilla.subscription.SubscriptionSpec
import io.crabzilla.util.PgTestContainer.pgConfig
import io.crabzilla.util.TestRepository
import io.crabzilla.writer.WriteResult
import io.crabzilla.writer.WriterApi
import io.crabzilla.writer.WriterApiImpl
import io.crabzilla.writer.WriterConfig
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import java.util.*

fun main() {
  val vertx = Vertx.vertx()
  val context = CrabzillaContextImpl(vertx, pgConfig())
  val testRepository = TestRepository(context.pgPool)
  val subscriptionName = "crabzilla.example1.customer.SimpleProjector"

  fun getWriter(): WriterApi<Customer, CustomerCommand, CustomerEvent> {
    val config =
      WriterConfig(
        initialStateFactory = CustomerInitialStateFactory(),
        evolveFunction = customerEvolveFunction,
        decideFunction = customerDecideFunction,
        eventSerDer = CustomerEventSerDer(),
        commandSerDer = CustomerCommandSerDer(),
        viewEffect = CustomerWriteResultViewEffect(),
      )
    return WriterApiImpl(context, config)
  }

  fun getSubscription(): SubscriptionApi {
    return SubscriptionComponentImpl(
      crabzillaContext = context,
      spec = SubscriptionSpec(subscriptionName),
      viewEffect = CustomersViewEffect(),
      viewTrigger = CustomersViewTrigger(vertx.eventBus()),
    ).extractApi()
  }

  vertx.eventBus().consumer<JsonObject>(CustomersViewTrigger.EVENTBUS_ADDRESS) { msg ->
    val viewAsJson = msg.body()
    println("**** triggered since this customer id not active anymore: " + viewAsJson.encodePrettily())
  }

  val subscriptionApi = getSubscription()

  getSubscription().deploy()
    .andThen {
      with(getWriter()) {
        fun handleCurried(targetStream: TargetStream): (CustomerCommand) -> Future<WriteResult<Customer, CustomerEvent>> {
          return { command -> handle(targetStream, command) }
        }
        val customerId = UUID.randomUUID()
        with(TargetStream(name = "Customer@$customerId")) {
          val handle = handleCurried(this)
          testRepository.cleanDatabase()
            .compose { handle(RegisterCustomer(customerId = customerId, name = "customer1")) }
            .compose { handle(ActivateCustomer("because it's needed")) }
            .compose { handle(DeactivateCustomer("because it's not needed")) }
        }
          .compose { subscriptionApi.handle() } // to for
          .onFailure { it.printStackTrace() }
          .onSuccess {
            testRepository.printOverview()
              .onFailure { it.printStackTrace() }
          }
      }
    }
}
