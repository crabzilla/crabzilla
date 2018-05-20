package io.github.crabzilla.example1

import com.zaxxer.hikari.HikariDataSource
import dagger.Component
import io.github.crabzilla.vertx.modules.qualifiers.ProjectionDatabase
import io.github.crabzilla.vertx.UnitOfWorkRepository
import io.github.crabzilla.vertx.verticles.ProjectionVerticle
import javax.inject.Singleton

@Singleton
@Component(modules = [ProjectorServiceModule::class])
interface ProjectorServiceComponent {

  fun projectorVerticles(): Set<ProjectionVerticle<out Any>>

  fun projectionRepo(): UnitOfWorkRepository

  @ProjectionDatabase
  fun datasource(): HikariDataSource

}
