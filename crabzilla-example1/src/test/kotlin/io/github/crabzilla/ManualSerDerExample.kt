package io.github.crabzilla

import io.github.crabzilla.command.CommandHandler
import io.github.crabzilla.command.CommandHandlerConfig
import io.github.crabzilla.command.CommandHandlerImpl
import io.github.crabzilla.command.CommandHandlerResult
import io.github.crabzilla.context.CrabzillaContextImpl
import io.github.crabzilla.example1.customer.effects.CustomerGivenAllEventsViewEffect
import io.github.crabzilla.example1.customer.effects.CustomersViewEffect
import io.github.crabzilla.example1.customer.effects.CustomersViewTrigger
import io.github.crabzilla.example1.customer.model.Customer
import io.github.crabzilla.example1.customer.model.CustomerCommand
import io.github.crabzilla.example1.customer.model.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.model.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.customer.model.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.model.CustomerEvent
import io.github.crabzilla.example1.customer.model.customerDecideFunction
import io.github.crabzilla.example1.customer.model.customerEvolveFunction
import io.github.crabzilla.example1.customer.serder.CustomerCommandSerDer
import io.github.crabzilla.example1.customer.serder.CustomerEventSerDer
import io.github.crabzilla.stream.TargetStream
import io.github.crabzilla.subscription.SubscriptionApi
import io.github.crabzilla.subscription.SubscriptionComponentImpl
import io.github.crabzilla.subscription.SubscriptionSpec
import io.github.crabzilla.util.PgTestContainer.pgConfig
import io.github.crabzilla.util.TestRepository
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import java.util.*

fun main() {
  val vertx = Vertx.vertx()
  val context = CrabzillaContextImpl(vertx, pgConfig())
  val testRepository = TestRepository(context.pgPool)
  val subscriptionName = "crabzilla.example1.customer.SimpleProjector"

  fun getCommandHandler(): CommandHandler<Customer, CustomerCommand, CustomerEvent> {
    val config =
      CommandHandlerConfig(
        initialState = Customer.Initial,
        evolveFunction = customerEvolveFunction,
        decideFunction = customerDecideFunction,
        eventSerDer = CustomerEventSerDer(),
        commandSerDer = CustomerCommandSerDer(),
        viewEffect = CustomerGivenAllEventsViewEffect(),
      )
    return CommandHandlerImpl(context, config)
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
      with(getCommandHandler()) {
        fun handleCurried(targetStream: TargetStream): (CustomerCommand) -> Future<CommandHandlerResult<Customer, CustomerEvent>> {
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
