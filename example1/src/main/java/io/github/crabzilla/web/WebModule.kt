package io.github.crabzilla.web

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import io.github.crabzilla.vertx.ReadDatabase
import io.github.crabzilla.vertx.WriteDatabase
import io.reactiverse.pgclient.PgPool
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.ext.healthchecks.Status

@Module
class WebModule {

  @Provides
  @IntoMap
  @StringKey("read-database")
  fun healthcheck1(@ReadDatabase readPool: PgPool) : Handler<Future<Status>> {
    return Handler { future: Future<Status> ->
        readPool.query("select 1") { ar ->
          if (ar.succeeded()) {
            future.succeeded()
          } else {
            future.fail(ar.cause())
          }
          readPool.close()
        }
    }
  }

  @Provides
  @IntoMap
  @StringKey("write-database")
  fun healthcheck2(@WriteDatabase writePool: PgPool) : Handler<Future<Status>> {
    return Handler { future: Future<Status> ->
      writePool.query("select 1") { ar ->
        if (ar.succeeded()) {
          future.succeeded()
        } else {
          future.fail(ar.cause())
        }
        writePool.close()
      }
    }
  }

}
