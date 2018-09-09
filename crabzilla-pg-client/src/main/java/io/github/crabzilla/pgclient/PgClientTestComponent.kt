package io.github.crabzilla.pgclient

import dagger.Component
import io.github.crabzilla.vertx.CrabzillaMainModule
import io.github.crabzilla.vertx.ReadDatabase
import io.github.crabzilla.vertx.WriteDatabase
import io.reactiverse.pgclient.PgPool
import io.vertx.core.Vertx
import javax.inject.Singleton

@Singleton
@Component(modules = [CrabzillaMainModule::class, PgClientModule::class])
interface PgClientTestComponent {

  fun vertx(): Vertx

  @ReadDatabase
  fun readDb() : PgPool

  @WriteDatabase
  fun writeDb() : PgPool

}
