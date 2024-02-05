package io.github.crabzilla.subscription

import io.github.crabzilla.TestRepository
import io.github.crabzilla.context.CrabzillaContext
import io.github.crabzilla.context.CrabzillaContextImpl
import io.github.crabzilla.example1.customer.Customer
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.CustomerCommandSerDer
import io.github.crabzilla.example1.customer.CustomerEventSerDer
import io.github.crabzilla.example1.customer.CustomersWriteViewEffect
import io.github.crabzilla.example1.customer.customerCommandHandler
import io.github.crabzilla.example1.customer.customerEventHandler
import io.github.crabzilla.writer.WriterApi
import io.github.crabzilla.writer.WriterApiImpl
import io.github.crabzilla.writer.WriterConfig
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.MountableFile

@ExtendWith(VertxExtension::class)
open class AbstractSubscriptionTest {
  var dbConfig: JsonObject

  init {
    postgresqlContainer.start()
    val dbUri: String = postgresqlContainer.jdbcUrl.substringAfter("jdbc:")
    dbConfig =
      JsonObject()
        .put("uri", dbUri)
        .put("username", DB_USERNAME)
        .put("password", DB_PASSWORD)
  }

  lateinit var context: CrabzillaContext
  lateinit var writer: WriterApi<CustomerCommand>
  lateinit var testRepository: TestRepository

  val customerConfig =
    WriterConfig(
      initialState = Customer.Initial,
      eventHandler = customerEventHandler,
      commandHandler = customerCommandHandler,
      eventSerDer = CustomerEventSerDer(),
      commandSerDer = CustomerCommandSerDer(),
      viewEffect = CustomersWriteViewEffect(),
    )

  @BeforeEach
  fun setup(
    vertx: Vertx,
    tc: VertxTestContext,
  ) {
    context = CrabzillaContextImpl(vertx, dbConfig)
    writer = WriterApiImpl(context, customerConfig)
    testRepository = TestRepository(context.pgPool)

    testRepository.cleanDatabase()
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  @AfterEach
  fun after(tc: VertxTestContext) {
    testRepository.printOverview()
      .onFailure { tc.failNow(it) }
      .onSuccess { tc.completeNow() }
  }

  companion object {
    private const val PG_DOCKER_IMAGE = "postgres:16"
    private const val DB_NAME = "crabzilla"
    private const val DB_USERNAME = "user1"
    private const val DB_PASSWORD = "pwd1"
    val postgresqlContainer =
      PostgreSQLContainer<Nothing>(PG_DOCKER_IMAGE)
        .apply {
          withDatabaseName(DB_NAME)
          withUsername(DB_USERNAME)
          withPassword(DB_PASSWORD)
          withCopyFileToContainer(
            MountableFile.forClasspathResource("./crabzilla.sql"),
            "/docker-entrypoint-initdb.d/crabzilla.sql",
          )
          withCopyFileToContainer(
            MountableFile.forClasspathResource("./example1.sql"),
            "/docker-entrypoint-initdb.d/example1.sql",
          )
        }
    val SUBSCRIPTION_1 = "crabzilla.example1.customer.SimpleProjector"
  }
}
