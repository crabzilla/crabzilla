package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import dagger.Component
import io.github.crabzilla.vertx.CrabzillaRestVerticle
import io.github.crabzilla.vertx.ReadDatabase
import io.github.crabzilla.vertx.WriteDatabase
import javax.inject.Singleton

// tag::component[]
@Singleton
@Component(modules = [(RestServiceModule::class)])
interface RestServiceComponent {

  fun restVerticle(): CrabzillaRestVerticle

  @WriteDatabase
  fun writeDatasource(): HikariDataSource

  @ReadDatabase
  fun readDatasource(): HikariDataSource
}

// end::component[]
