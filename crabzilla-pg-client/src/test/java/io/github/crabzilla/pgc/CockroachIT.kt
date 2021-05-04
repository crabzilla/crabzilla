package io.github.crabzilla.pgc

import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.pgclient.SslMode
import io.vertx.sqlclient.PoolOptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
@Disabled
class CockroachIT {

  // https://www.cockroachlabs.com/docs/v20.2/build-a-java-app-with-cockroachdb.html?filters=local
  // https://forum.cockroachlabs.com/t/java-non-blocking-driver-for-vertx-io/1123/9
  // cockroach demo --insecure

  val connectOptions = PgConnectOptions()
//  .setPort(26257)
    // .setPort(38375)
    .setPort(39269)
    .setHost("127.208.238.251")
    .setDatabase("movr")
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

    println("Test")
    pgPool.query("select * from users").execute()
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
