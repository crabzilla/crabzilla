package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import dagger.Component
import io.github.crabzilla.core.Entity
import io.github.crabzilla.vertx.*
import io.github.crabzilla.vertx.handler.CommandHandlerVerticle
import io.github.crabzilla.vertx.projector.ProjectionHandlerVerticle
import javax.inject.Singleton

// tag::component[]
@Singleton
@Component(modules = [(MonolithServiceModule::class)])
interface MonolithServiceComponent {

  @WriteDatabase
  fun writeDatasource(): HikariDataSource

  @ReadDatabase
  fun readDatasource(): HikariDataSource

  @ProjectionDatabase
  fun datasource(): HikariDataSource

  fun restVerticles(): Set<CommandRestVerticle>
  fun handlerVerticles(): Set<CommandHandlerVerticle<out Entity>>
  fun projectorVerticles(): Set<ProjectionHandlerVerticle<out Any>>

  fun projectionRepo(): UnitOfWorkRepository

}

// end::component[]
