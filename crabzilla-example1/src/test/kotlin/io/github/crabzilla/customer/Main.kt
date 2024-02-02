package io.github.crabzilla.customer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.crabzilla.PgTestContainer.pgConfig
import io.github.crabzilla.PgTestContainer.postgresqlContainer
import io.github.crabzilla.TestRepository
import io.github.crabzilla.context.CrabzillaContextImpl
import io.github.crabzilla.context.TargetStream
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerEvent
import io.github.crabzilla.example1.customer.customerCommandHandler
import io.github.crabzilla.example1.customer.customerEventHandler
import io.github.crabzilla.jackson.JacksonJsonObjectSerDer
import io.github.crabzilla.writer.CrabzillaWriterConfig
import io.github.crabzilla.writer.CrabzillaWriterImpl
import io.vertx.core.Vertx
import java.util.*

fun main() {
  postgresqlContainer.start()

  val json: ObjectMapper = jacksonObjectMapper().findAndRegisterModules().enable(SerializationFeature.INDENT_OUTPUT)
  val vertx = Vertx.vertx()
  val context = CrabzillaContextImpl(vertx, pgConfig())
  val testRepository = TestRepository(context.pgPool)

  val config =
    CrabzillaWriterConfig(
      initialState = Customer.Initial,
      eventHandler = customerEventHandler,
      commandHandler = customerCommandHandler,
      eventSerDer = JacksonJsonObjectSerDer(json, clazz = CustomerEvent::class),
      commandSerDer = JacksonJsonObjectSerDer(json, clazz = CustomerCommand::class),
    )

  with(CrabzillaWriterImpl(context, config)) {
    val id = UUID.randomUUID()
    val ts = TargetStream(name = "Customer@$id")
    testRepository.cleanDatabase()
      .compose { handle(ts, CustomerCommand.RegisterCustomer(customerId = id, name = "customer1")) }
      .compose { handle(ts, CustomerCommand.ActivateCustomer("because it's needed")) }
      .compose { handle(ts, CustomerCommand.DeactivateCustomer("because it's not needed")) }
//        .compose { handle(this, ActivateCustomer("because it's needed")) }
//        .compose { handle(this, DeactivateCustomer("because it's not needed")) }
//        .compose { handle(this, ActivateCustomer("because it's needed")) }
  }
    .onFailure { it.printStackTrace() }
    .onSuccess {
      testRepository.printOverview()
        .onFailure { it.printStackTrace() }
    }
}
