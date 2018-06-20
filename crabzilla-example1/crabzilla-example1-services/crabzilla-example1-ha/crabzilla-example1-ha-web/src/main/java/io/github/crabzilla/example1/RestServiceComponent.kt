package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import dagger.Component
import io.github.crabzilla.vertx.qualifiers.ReadDatabase
import io.github.crabzilla.vertx.qualifiers.WriteDatabase
import io.github.crabzilla.vertx.verticles.RestVerticle
import javax.inject.Singleton

@Singleton
@Component(modules = [(RestServiceModule::class)])
interface RestServiceComponent {

  fun restVerticle(): RestVerticle

  @WriteDatabase
  fun writeDatasource(): HikariDataSource

  @ReadDatabase
  fun readDatasource(): HikariDataSource
}
