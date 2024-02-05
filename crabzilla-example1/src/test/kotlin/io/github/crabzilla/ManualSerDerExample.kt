package io.github.crabzilla

import io.github.crabzilla.context.CrabzillaContextImpl
import io.github.crabzilla.context.EventMetadata
import io.github.crabzilla.context.TargetStream
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomerCommandSerDer
import io.github.crabzilla.example1.customer.CustomerEventSerDer
import io.github.crabzilla.example1.customer.CustomersViewEffect
import io.github.crabzilla.example1.customer.CustomersViewTrigger
import io.github.crabzilla.example1.customer.CustomersWriteViewEffect
import io.github.crabzilla.example1.customer.customerCommandHandler
import io.github.crabzilla.example1.customer.customerEventHandler
import io.github.crabzilla.subscription.SubscriptionApi
import io.github.crabzilla.subscription.SubscriptionComponentImpl
import io.github.crabzilla.subscription.SubscriptionSpec
import io.github.crabzilla.util.PgTestContainer.pgConfig
import io.github.crabzilla.util.TestRepository
import io.github.crabzilla.writer.WriterApi
import io.github.crabzilla.writer.WriterApiImpl
import io.github.crabzilla.writer.WriterConfig
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import java.util.*

fun main() {
  val vertx = Vertx.vertx()
  val context = CrabzillaContextImpl(vertx, pgConfig())
  val testRepository = TestRepository(context.pgPool)
  val subscriptionName = "crabzilla.example1.customer.SimpleProjector"

  fun getWriter(): WriterApi<CustomerCommand> {
    val config =
      WriterConfig(
        initialState = Customer.Initial,
        eventHandler = customerEventHandler,
        commandHandler = customerCommandHandler,
        eventSerDer = CustomerEventSerDer(),
        commandSerDer = CustomerCommandSerDer(),
        viewEffect = CustomersWriteViewEffect(),
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
        fun handleCurried(targetStream: TargetStream): (CustomerCommand) -> Future<EventMetadata> {
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
