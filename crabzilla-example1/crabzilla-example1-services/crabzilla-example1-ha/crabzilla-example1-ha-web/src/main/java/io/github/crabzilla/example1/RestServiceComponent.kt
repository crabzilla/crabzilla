package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import dagger.Component
import io.github.crabzilla.vertx.modules.qualifiers.ReadDatabase
import io.github.crabzilla.vertx.modules.qualifiers.WriteDatabase
import io.github.crabzilla.vertx.verticles.CrabzillaVerticle
import javax.inject.Singleton

@Singleton
@Component(modules = [(RestServiceModule::class)])
interface RestServiceComponent {

  fun restVerticle(): CrabzillaVerticle

  @WriteDatabase
  fun writeDatasource(): HikariDataSource

  @ReadDatabase
  fun readDatasource(): HikariDataSource
}
