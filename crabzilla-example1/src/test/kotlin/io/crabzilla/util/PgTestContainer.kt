package io.crabzilla.util

import io.vertx.core.json.JsonObject
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.MountableFile

object PgTestContainer {
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
          MountableFile.forClasspathResource("crabzilla.sql"),
          "/docker-entrypoint-initdb.d/crabzilla.sql",
        )
        withCopyFileToContainer(
          MountableFile.forClasspathResource("example1.sql"),
          "/docker-entrypoint-initdb.d/example1.sql",
        )
      }

  fun pgConfig(): JsonObject {
    postgresqlContainer.start()
    val dbUri: String = postgresqlContainer.jdbcUrl.substringAfter("jdbc:")
    return JsonObject()
      .put("uri", dbUri)
      .put("username", DB_USERNAME)
      .put("password", DB_PASSWORD)
  }
}
