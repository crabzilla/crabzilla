package io.crabzilla.context

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.PostgreSQLContainer

@DisplayName("Instantiating CrabzillaContext")
@ExtendWith(VertxExtension::class)
class CrabzillaContextTest {
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

  @Test
  fun `it can instantiate context`(vertx: Vertx) {
    val context = CrabzillaContextImpl(vertx, dbConfig)
    assertThat(context).isNotNull()
  }

  @Test
  fun `it create a subscriber`(vertx: Vertx) {
    val context = CrabzillaContextImpl(vertx, dbConfig)
    assertThat(context.newPgSubscriber()).isNotNull
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
