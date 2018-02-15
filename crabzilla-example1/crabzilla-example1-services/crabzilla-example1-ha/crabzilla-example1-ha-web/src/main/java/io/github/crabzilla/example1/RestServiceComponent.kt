package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import dagger.Component
import io.github.crabzilla.vertx.entity.EntityCommandRestVerticle
import io.github.crabzilla.vertx.modules.ReadDatabase
import io.github.crabzilla.vertx.modules.WriteDatabase
import javax.inject.Singleton

// tag::component[]
@Singleton
@Component(modules = [(RestServiceModule::class)])
interface RestServiceComponent {

  fun restVerticles(): Set<EntityCommandRestVerticle>

  @WriteDatabase
  fun writeDatasource(): HikariDataSource

  @ReadDatabase
  fun readDatasource(): HikariDataSource
}

// end::component[]
