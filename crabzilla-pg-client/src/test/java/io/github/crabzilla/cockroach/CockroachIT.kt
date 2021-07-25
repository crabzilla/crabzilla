package io.github.crabzilla.cockroach

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.pgclient.SslMode
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
@Disabled
class CockroachIT {

  // https://github.com/cockroachlabs-field/docker-examples/blob/master/example-single-node/docker-compose.yml
  // https://www.cockroachlabs.com/docs/v20.2/build-a-java-app-with-cockroachdb.html?filters=local
  // https://forum.cockroachlabs.com/t/java-non-blocking-driver-for-vertx-io/1123/9
  // cockroach demo --insecure

  val connectOptions = PgConnectOptions()
//  .setPort(26257)
    // .setPort(38375)
    .setPort(26257)
    .setHost("0.0.0.0")
    .setDatabase("test")
    .setUser("root")
    .setPassword("admin")
    .setSsl(false)
    .setSslMode(SslMode.DISABLE)

  val poolOptions = PoolOptions().setMaxSize(5)

  lateinit var pgPool: PgPool

  @BeforeEach
  fun setup(vertx: Vertx, tc: VertxTestContext) {
    pgPool = PgPool.pool(vertx, connectOptions, poolOptions)
    println("Init")
    tc.completeNow()
  }

  @Test
  @DisplayName("cock")
  fun a0(tc: VertxTestContext, vertx: Vertx) {

    val sql1 = """CREATE TABLE IF NOT EXISTS programming (id UUID DEFAULT uuid_v4()::UUID PRIMARY KEY, posts JSONB)"""
    val sql2 = "INSERT into programming (posts) values ($1)"
    val json = JsonObject().put("subject", "kafka")

    println("Test")
    pgPool
      .preparedQuery(sql1)
      .execute()
      .onFailure { tc.failNow(it) }
      .compose {
        pgPool
          .preparedQuery(sql2)
          .execute(Tuple.of(json))
      }.compose {
        pgPool
          .query("SELECT * from programming")
          .execute()
          .onSuccess { rs ->
            rs.forEach { r ->
              println(r.toJson())
            }
            tc.completeNow()
          }.onFailure {
            tc.failNow(it)
          }
      }
  }
}
