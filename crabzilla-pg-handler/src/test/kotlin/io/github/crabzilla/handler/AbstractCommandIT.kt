package io.github.crabzilla.handler

import io.github.crabzilla.TestRepository
import io.github.crabzilla.context.CrabzillaContext
import io.github.crabzilla.context.CrabzillaContextImpl
import io.github.crabzilla.example1.customer.CustomerCommand
import io.github.crabzilla.example1.customer.customerConfig
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.MountableFile

@ExtendWith(VertxExtension::class)
open class AbstractCommandIT {
  var dbConfig: JsonObject

  init {
    postgresqlContainer.start()
    val dbUri: String = postgresqlContainer.jdbcUrl.substringAfter("jdbc:")
    dbConfig =
      JsonObject()
        .put("uri", dbUri)
        .put("username", DB_USERNAME)
        .put("password", DB_PASSWORD)
    println(dbConfig.encodePrettily())
  }

  lateinit var context: CrabzillaContext
  lateinit var crabzillaHandler: CrabzillaHandler<CustomerCommand>
  lateinit var testRepository: TestRepository

  @BeforeEach
  fun setup(
    vertx: Vertx,
    tc: VertxTestContext,
  ) {
    context = CrabzillaContextImpl(vertx, dbConfig)
    crabzillaHandler = CrabzillaHandlerImpl(context, customerConfig)
    testRepository = TestRepository(context.pgPool)

    TestRepository.cleanDatabase(context.pgPool)
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
  }
}
