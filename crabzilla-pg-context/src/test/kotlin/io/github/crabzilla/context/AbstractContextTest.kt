package io.github.crabzilla.context

import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.PostgreSQLContainer
import java.util.*

@ExtendWith(VertxExtension::class)
abstract class AbstractContextTest {
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

  companion object {
    private const val PG_DOCKER_IMAGE = "postgres:16-alpine"
    private const val DB_NAME = "crabzilla"
    private const val DB_USERNAME = "user1"
    private const val DB_PASSWORD = "pwd1"
    val postgresqlContainer =
      PostgreSQLContainer<Nothing>(PG_DOCKER_IMAGE)
        .apply {
          withDatabaseName(DB_NAME)
          withUsername(DB_USERNAME)
          withPassword(DB_PASSWORD)
        }
  }
}
