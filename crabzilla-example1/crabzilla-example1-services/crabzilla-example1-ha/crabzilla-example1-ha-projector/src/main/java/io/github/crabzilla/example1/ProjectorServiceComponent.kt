package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import dagger.Component
import io.github.crabzilla.vertx.ProjectionDatabase
import io.github.crabzilla.vertx.UnitOfWorkRepository
import io.github.crabzilla.vertx.projection.ProjectionHandlerVerticle
import javax.inject.Singleton

// tag::component[]
@Singleton
@Component(modules = arrayOf(ProjectorServiceModule::class))
interface ProjectorServiceComponent {

  fun projectorVerticles(): Set<ProjectionHandlerVerticle<out Any>>

  fun projectionRepo(): UnitOfWorkRepository

  @ProjectionDatabase
  fun datasource(): HikariDataSource

}

// end::component[]
