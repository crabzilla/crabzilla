package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import dagger.Component
import io.github.crabzilla.vertx.CrabzillaRestVerticle
import io.github.crabzilla.vertx.modules.qualifiers.ReadDatabase
import io.github.crabzilla.vertx.modules.qualifiers.WriteDatabase
import javax.inject.Singleton

@Singleton
@Component(modules = [(RestServiceModule::class)])
interface RestServiceComponent {

  fun restVerticle(): CrabzillaRestVerticle

  @WriteDatabase
  fun writeDatasource(): HikariDataSource

  @ReadDatabase
  fun readDatasource(): HikariDataSource
}
