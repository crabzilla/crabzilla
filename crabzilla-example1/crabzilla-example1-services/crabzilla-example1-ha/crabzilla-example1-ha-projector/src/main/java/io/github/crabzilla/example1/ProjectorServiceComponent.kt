package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import dagger.Component
import io.github.crabzilla.vertx.entity.EntityUnitOfWorkRepository
import io.github.crabzilla.vertx.modules.qualifiers.ProjectionDatabase
import io.github.crabzilla.vertx.projection.ProjectionHandlerVerticle
import javax.inject.Singleton

// tag::component[]
@Singleton
@Component(modules = [ProjectorServiceModule::class])
interface ProjectorServiceComponent {

  fun projectorVerticles(): Set<ProjectionHandlerVerticle<out Any>>

  fun projectionRepo(): EntityUnitOfWorkRepository

  @ProjectionDatabase
  fun datasource(): HikariDataSource

}

// end::component[]
