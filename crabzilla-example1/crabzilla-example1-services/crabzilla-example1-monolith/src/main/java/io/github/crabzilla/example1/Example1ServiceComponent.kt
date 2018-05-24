package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import dagger.Component
import io.github.crabzilla.core.Entity
import io.github.crabzilla.vertx.UnitOfWorkRepository
import io.github.crabzilla.vertx.modules.qualifiers.ProjectionDatabase
import io.github.crabzilla.vertx.modules.qualifiers.ReadDatabase
import io.github.crabzilla.vertx.modules.qualifiers.WriteDatabase
import io.github.crabzilla.vertx.projector.JdbiProjectorVerticle
import io.github.crabzilla.vertx.verticles.CommandVerticle
import io.github.crabzilla.vertx.verticles.CrabzillaVerticle
import javax.inject.Singleton

@Singleton
@Component(modules = [Example1ServiceModule::class])
interface Example1ServiceComponent {

  @WriteDatabase
  fun writeDatasource(): HikariDataSource

  @ReadDatabase
  fun readDatasource(): HikariDataSource

  @ProjectionDatabase
  fun datasource(): HikariDataSource

  fun restVerticles(): CrabzillaVerticle
  fun projectionRepo(): UnitOfWorkRepository

  fun handlerVerticles(): Set<CommandVerticle<out Entity>>
  fun projectorVerticles(): Set<JdbiProjectorVerticle<out Any>>

}
