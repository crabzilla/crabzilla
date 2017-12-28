package io.github.crabzilla.example1

import dagger.Component
import io.github.crabzilla.vertx.projection.EventsProjectionVerticle
import javax.inject.Singleton

// tag::component[]
@Singleton
@Component(modules = [ProjectorServiceModule::class])
interface ProjectorComponent {

  fun projectorVerticles(): Set<EventsProjectionVerticle<out Any>>
}

// end::component[]