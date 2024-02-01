package io.github.crabzilla.customer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.crabzilla.TestRepository
import io.github.crabzilla.context.CrabzillaContextImpl
import io.github.crabzilla.context.EventMetadata
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.customerCommandHandler
import io.github.crabzilla.example1.customer.customerEventHandler
import io.github.crabzilla.handler.CrabzillaHandlerConfig
import io.github.crabzilla.handler.CrabzillaHandlerImpl
import io.github.crabzilla.handler.TargetStream
import io.github.crabzilla.jackson.JacksonJsonObjectSerDer
import io.vertx.core.Future
import io.vertx.core.Vertx
import java.util.*

fun main() {
  val json: ObjectMapper = jacksonObjectMapper().findAndRegisterModules().enable(SerializationFeature.INDENT_OUTPUT)
  val vertx = Vertx.vertx()
  val context = CrabzillaContextImpl(vertx, TestRepository.DATABASE_CONFIG)
  val testRepository = TestRepository(context.pgPool)

  val config =
    CrabzillaHandlerConfig(
      initialState = Customer.Initial,
      eventHandler = customerEventHandler,
      commandHandler = customerCommandHandler,
      eventSerDer = JacksonJsonObjectSerDer(json, clazz = CustomerEvent::class),
      commandSerDer = JacksonJsonObjectSerDer(json, clazz = CustomerCommand::class),
      eventProjector = CustomerEventProjector(),
    )

  with(CrabzillaHandlerImpl(context, config)) {
    fun handleCurried(targetStream: TargetStream): (CustomerCommand) -> Future<EventMetadata> {
      return { command -> handle(targetStream, command) }
    }
    val id = UUID.randomUUID()
    with(TargetStream(name = "Customer.$id")) {
      val handle = handleCurried(this)
      testRepository.cleanDatabase()
        .compose { handle(RegisterCustomer(customerId = id, name = "customer1")) }
//        .compose { handle(ActivateCustomer("because it's needed")) }
//        .compose { handle(DeactivateCustomer("because it's not needed")) }
    }
      .onFailure { it.printStackTrace() }
      .onSuccess {
        testRepository.printOverview()
          .onFailure { it.printStackTrace() }
      }
  }
}
