package io.github.crabzilla

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.crabzilla.context.CrabzillaContextImpl
import io.github.crabzilla.context.TargetStream
import io.github.crabzilla.customer.CustomerViewTrigger
import io.github.crabzilla.customer.CustomersAsyncViewEffect
import io.github.crabzilla.customer.CustomersSyncViewEffect
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.customerCommandHandler
import io.github.crabzilla.example1.customer.customerEventHandler
import io.github.crabzilla.jackson.JacksonJsonObjectSerDer
import io.github.crabzilla.subscription.SubscriptionApi
import io.github.crabzilla.subscription.SubscriptionComponentImpl
import io.github.crabzilla.subscription.SubscriptionSpec
import io.github.crabzilla.util.PgTestContainer.pgConfig
import io.github.crabzilla.util.TestRepository
import io.github.crabzilla.writer.WriterApi
import io.github.crabzilla.writer.WriterApiImpl
import io.github.crabzilla.writer.WriterConfig
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import java.util.*

fun main() {
  val objectMapper: ObjectMapper = jacksonObjectMapper().findAndRegisterModules().enable(SerializationFeature.INDENT_OUTPUT)
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
        eventSerDer = JacksonJsonObjectSerDer(objectMapper, clazz = CustomerEvent::class),
        commandSerDer = JacksonJsonObjectSerDer(objectMapper, clazz = CustomerCommand::class),
        viewEffect = CustomersSyncViewEffect(),
      )
    return WriterApiImpl(context, config)
  }

  fun getSubscription(): SubscriptionApi {
    return SubscriptionComponentImpl(
      crabzillaContext = context,
      spec = SubscriptionSpec(subscriptionName),
      viewEffect = CustomersAsyncViewEffect(),
      viewTrigger = CustomerViewTrigger(vertx.eventBus()),
    ).extractApi()
  }

  vertx.eventBus().consumer<JsonObject>(CustomerViewTrigger.EVENTBUS_ADDRESS) { msg ->
    val viewAsJson = msg.body()
    println("**** triggered since this customer id not active anymore: " + viewAsJson.encodePrettily())
  }

  val subscriptionApi = getSubscription()

  getSubscription().deploy()
    .andThen {
      val writerApi = getWriter()
      with(writerApi) {
        val customerId = UUID.randomUUID()
        val targetStream = TargetStream(name = "Customer@$customerId")
        testRepository.cleanDatabase()
          .compose { handle(targetStream, RegisterCustomer(customerId = customerId, name = "customer1")) }
          .compose { handle(targetStream, ActivateCustomer("because it's needed")) }
          .compose { handle(targetStream, DeactivateCustomer("because it's not needed")) }
          .compose { handle(targetStream, ActivateCustomer("because it's needed")) }
          .compose { handle(targetStream, DeactivateCustomer("because it's not needed")) }
      }
        .compose { subscriptionApi.handle() } // to force it to work since it's async
        .onFailure { it.printStackTrace() }
        .onSuccess {
          testRepository.printOverview()
            .onFailure { it.printStackTrace() }
        }
    }
}
