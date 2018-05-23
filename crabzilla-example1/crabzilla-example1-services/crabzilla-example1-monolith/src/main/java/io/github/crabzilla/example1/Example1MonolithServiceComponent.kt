package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import dagger.Component
import io.github.crabzilla.core.Entity
import io.github.crabzilla.vertx.*
import io.github.crabzilla.vertx.modules.qualifiers.ProjectionDatabase
import io.github.crabzilla.vertx.modules.qualifiers.ReadDatabase
import io.github.crabzilla.vertx.modules.qualifiers.WriteDatabase
import io.github.crabzilla.vertx.verticles.CommandVerticle
import io.github.crabzilla.vertx.projector.JdbiProjectorVerticle
import io.github.crabzilla.vertx.verticles.CrabzillaRestVerticle
import javax.inject.Singleton

// tag::component[]
@Singleton
@Component(modules = [(Example1MonolithServiceModule::class)])
interface Example1MonolithServiceComponent {

  @WriteDatabase
  fun writeDatasource(): HikariDataSource

  @ReadDatabase
  fun readDatasource(): HikariDataSource

  @ProjectionDatabase
  fun datasource(): HikariDataSource

  fun restVerticles(): CrabzillaRestVerticle
  fun projectionRepo(): UnitOfWorkRepository

  fun handlerVerticles(): Set<CommandVerticle<out Entity>>
  fun projectorVerticles(): Set<JdbiProjectorVerticle<out Any>>

}

// end::component[]
