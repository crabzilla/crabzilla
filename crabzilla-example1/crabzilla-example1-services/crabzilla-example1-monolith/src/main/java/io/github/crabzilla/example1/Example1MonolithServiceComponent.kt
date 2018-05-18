package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import dagger.Component
import io.github.crabzilla.core.Entity
import io.github.crabzilla.vertx.*
import io.github.crabzilla.vertx.CommandHandlerVerticle
import io.github.crabzilla.vertx.projector.ProjectionHandlerVerticle
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

  fun handlerVerticles(): Set<CommandHandlerVerticle<out Entity>>
  fun projectorVerticles(): Set<ProjectionHandlerVerticle<out Any>>

}

// end::component[]
