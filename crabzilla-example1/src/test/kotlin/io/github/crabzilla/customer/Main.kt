package io.github.crabzilla.customer

import DefaultCrabzillaContextFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.crabzilla.command.CommandComponentConfig
import io.github.crabzilla.command.DefaultCommandComponent
import io.github.crabzilla.customer.CustomerCommand.ActivateCustomer
import io.github.crabzilla.customer.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.jackson.JacksonJsonObjectSerDer
import io.vertx.core.Vertx

val json: ObjectMapper = jacksonObjectMapper().findAndRegisterModules().enable(SerializationFeature.INDENT_OUTPUT)

// TODO https://www.profissionaisti.com.br/apache-kafka-kubernetes-zookeeper/

fun main() {
  val vertx = Vertx.vertx()
  val context = DefaultCrabzillaContextFactory().new(vertx, TestRepository.DATABASE_CONFIG)

  val customerConfig =
    CommandComponentConfig(
      stateClass = Customer::class,
      commandSerDer = JacksonJsonObjectSerDer(json, clazz = CustomerCommand::class),
      eventSerDer = JacksonJsonObjectSerDer(json, clazz = CustomerEvent::class),
      eventHandler = customerEventHandler,
      commandHandler = customerCommandHandler,
      initialState = Customer.Initial,
    )
  val commandComponent = DefaultCommandComponent(context, customerConfig)

  val customerId = "C1"

  commandComponent.handle(customerId, RegisterCustomer(customerId = customerId, name = "customer1"))
    .compose { commandComponent.handle(customerId, ActivateCustomer("because it's needed")) }
    .compose { commandComponent.handle(customerId, DeactivateCustomer("because it's not needed")) }
    .compose { commandComponent.handle(customerId, ActivateCustomer("because it's needed")) }
    .compose { commandComponent.handle(customerId, DeactivateCustomer("because it's not needed")) }
    .compose { commandComponent.handle(customerId, ActivateCustomer("because it's needed")) }
    .onFailure { it.printStackTrace() }
    .onSuccess {
      val testRepository = TestRepository(context.pgPool())
      testRepository
        .getAllEvents()
        .onSuccess { events -> events.forEach { println(it) } }
        .onFailure { it.printStackTrace() }
    }

  println("*** bye")
}
