package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import dagger.Component
import io.github.crabzilla.vertx.projection.ProjectionHandlerVerticle
import io.github.crabzilla.vertx.projection.ProjectionRepository
import io.github.crabzilla.vertx.modules.qualifiers.ProjectionDatabase
import javax.inject.Singleton

// tag::component[]
@Singleton
@Component(modules = [ProjectorServiceModule::class])
interface ProjectorServiceComponent {

  fun projectorVerticles(): Set<ProjectionHandlerVerticle<out Any>>

  fun projectionRepo(): ProjectionRepository

  @ProjectionDatabase
  fun datasource(): HikariDataSource

}

// end::component[]
