package io.github.crabzilla.example1

import dagger.Component
import io.github.crabzilla.vertx.projection.ProjectionHandlerVerticle
import io.github.crabzilla.vertx.projection.ProjectionRepository
import javax.inject.Singleton

// tag::component[]
@Singleton
@Component(modules = [ProjectorServiceModule::class])
interface ProjectorComponent {

  fun projectorVerticles(): Set<ProjectionHandlerVerticle<out Any>>

  fun projectionRepo(): ProjectionRepository

}

// end::component[]