package io.github.crabzilla

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.crabzilla.context.CrabzillaContextImpl
import io.github.crabzilla.context.EventMetadata
import io.github.crabzilla.context.TargetStream
import io.github.crabzilla.customer.CustomerCommandSerDer
import io.github.crabzilla.customer.CustomerEventSerDer
import io.github.crabzilla.customer.CustomersViewEffect
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerCommand.ActivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.DeactivateCustomer
import io.github.crabzilla.example1.customer.CustomerCommand.RegisterCustomer
import io.github.crabzilla.example1.customer.customerCommandHandler
import io.github.crabzilla.example1.customer.customerEventHandler
import io.github.crabzilla.util.PgTestContainer.pgConfig
import io.github.crabzilla.util.PgTestContainer.postgresqlContainer
import io.github.crabzilla.util.TestRepository
import io.github.crabzilla.writer.WriterApiImpl
import io.github.crabzilla.writer.WriterConfig
import io.vertx.core.Future
import io.vertx.core.Vertx
import java.util.*

fun main() {
  postgresqlContainer.start()

  val json: ObjectMapper = jacksonObjectMapper().findAndRegisterModules().enable(SerializationFeature.INDENT_OUTPUT)
  val vertx = Vertx.vertx()
  val context = CrabzillaContextImpl(vertx, pgConfig())
  val testRepository = TestRepository(context.pgPool)

  val config =
    WriterConfig(
      initialState = Customer.Initial,
      eventHandler = customerEventHandler,
      commandHandler = customerCommandHandler,
      eventSerDer = CustomerEventSerDer(),
      commandSerDer = CustomerCommandSerDer(),
      viewEffect = CustomersViewEffect(),
    )

  with(WriterApiImpl(context, config)) {
    fun handleCurried(targetStream: TargetStream): (CustomerCommand) -> Future<EventMetadata> {
      return { command -> handle(targetStream, command) }
    }
    val id = UUID.randomUUID()
    with(TargetStream(name = "Customer@$id")) {
      val handle = handleCurried(this)
      testRepository.cleanDatabase()
        .compose { handle(RegisterCustomer(customerId = id, name = "customer1")) }
        .compose { handle(ActivateCustomer("because it's needed")) }
        .compose { handle(DeactivateCustomer("because it's not needed")) }
    }
      .onFailure { it.printStackTrace() }
      .onSuccess {
        testRepository.printOverview()
          .onFailure { it.printStackTrace() }
      }
  }
}
