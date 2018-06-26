package io.github.crabzilla.vertx.pgclient

import dagger.Component
import io.github.crabzilla.vertx.qualifiers.ReadDatabase
import io.github.crabzilla.vertx.qualifiers.WriteDatabase
import io.reactiverse.pgclient.PgPool
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.ext.healthchecks.Status
import javax.inject.Singleton

@Singleton
@Component(modules = [PgClientModule::class])
interface PgClientComponent {

  fun vertx(): Vertx

  @ReadDatabase
  fun readDb() : PgPool

  @WriteDatabase
  fun writeDb() : PgPool

  fun healthHandlers() : Map<String, Handler<Future<Status>>>
}
