package io.github.crabzilla.customer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.crabzilla.context.CrabzillaContextImpl
import io.github.crabzilla.customer.CustomerCommand.ActivateCustomer
import io.github.crabzilla.customer.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.handler.CrabzillaHandlerConfig
import io.github.crabzilla.handler.CrabzillaHandlerImpl
import io.github.crabzilla.handler.TargetStream
import io.github.crabzilla.jackson.JacksonJsonObjectSerDer
import io.vertx.core.Vertx
import java.util.*

// TODO https://www.profissionaisti.com.br/apache-kafka-kubernetes-zookeeper/

fun main() {
  val json: ObjectMapper = jacksonObjectMapper().findAndRegisterModules().enable(SerializationFeature.INDENT_OUTPUT)
  val vertx = Vertx.vertx()
  val context = CrabzillaContextImpl(vertx, TestRepository.DATABASE_CONFIG)

  val config =
    CrabzillaHandlerConfig(
      initialState = Customer.Initial,
      eventHandler = customerEventHandler,
      commandHandler = customerCommandHandler,
      eventSerDer = JacksonJsonObjectSerDer(json, clazz = CustomerEvent::class),
      commandSerDer = JacksonJsonObjectSerDer(json, clazz = CustomerCommand::class),
    )

  with(CrabzillaHandlerImpl(context, config)) {
    with(TargetStream(stateType = "Customer", stateId = UUID.randomUUID().toString())) {
      handle(this, RegisterCustomer(customerId = this.stateId, name = "customer1"))
        .compose { handle(this, ActivateCustomer("because it's needed")) }
        .compose { handle(this, DeactivateCustomer("because it's not needed")) }
        .compose { handle(this, ActivateCustomer("because it's needed")) }
        .compose { handle(this, DeactivateCustomer("because it's not needed")) }
        .compose { handle(this, ActivateCustomer("because it's needed")) }
    }
      .onFailure { it.printStackTrace() }
      .onSuccess {
        val testRepository = TestRepository(context.pgPool)
        testRepository
          .getAllEvents()
          .onSuccess { events -> events.forEach { println(it) } }
          .onFailure { it.printStackTrace() }
      }
  }

  println("*** bye")
}
