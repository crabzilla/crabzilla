package io.github.crabzilla.example1

import dagger.Component
import io.github.crabzilla.core.entity.Entity
import io.github.crabzilla.example1.customer.CustomerModule
import io.github.crabzilla.vertx.entity.EntityCommandHandlerVerticle
import io.github.crabzilla.vertx.entity.EntityCommandRestVerticle
import io.github.crabzilla.vertx.modules.ReadDbModule
import io.github.crabzilla.vertx.modules.WriteDbModule
import javax.inject.Singleton

// tag::component[]
@Singleton
@Component(modules = [CustomerModule::class, Example1Module::class, WriteDbModule::class, ReadDbModule::class])
interface Example1Component {

  fun commandVerticles(): Set<EntityCommandHandlerVerticle<out Entity>>

  fun restVerticles(): Set<EntityCommandRestVerticle<out Any>>
}

// end::component[]