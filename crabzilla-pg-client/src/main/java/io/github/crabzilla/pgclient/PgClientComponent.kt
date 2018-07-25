package io.github.crabzilla.pgclient

import dagger.Component
import io.github.crabzilla.vertx.qualifiers.ReadDatabase
import io.github.crabzilla.vertx.qualifiers.WriteDatabase
import io.reactiverse.pgclient.PgPool
import io.vertx.core.Vertx
import javax.inject.Singleton

@Singleton
@Component(modules = [PgClientModule::class])
interface PgClientComponent {

  fun vertx(): Vertx

  @ReadDatabase
  fun readDb() : PgPool

  @WriteDatabase
  fun writeDb() : PgPool

}
